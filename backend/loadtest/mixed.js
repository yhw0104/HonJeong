// 테스트 6 — 혼합 부하. "실제로 앱을 쓰는 사용자 N명"이 서버에 주는 부하를 잰다.
//
// 왜 필요한가: 지금까지는 기능을 **따로만** 쟀다(검색 단독, 폴링 단독, 채팅 목록 단독).
// 그래서 "동시 사용자 몇 명까지 되는가"에 답할 수 없었다. 검색 150건/초와 폴링 300명은
// 각각 CPU 20%였지만, 둘이 같은 2코어를 나눠 쓰면 어디서 만나는지는 재본 적이 없다.
//
// ★1 iteration = 한 사용자의 "15초" 활동이다. polling.js와 같은 단위를 쓴다 —
//   rate: N, timeUnit: '15s' 로 두면 N이 그대로 **동시 접속자 수**가 된다.
//   iteration 안에서 sleep 하지 않으므로 VU가 적게 들어 수천 명까지 흉내낼 수 있다.
//
// 한 주기(15초)에 사용자가 하는 일:
//   - 폴링 6개 (100%)          — 앱이 15초마다 무조건 보낸다. MainTabs·MapHome·BellButton
//   - 검색 세션 (P_SEARCH)     — 2글자 → 3글자로 좁혀 치는 것을 흉내낸다(요청 2건)
//   - 식당 상세 (P_DETAIL)     — 결과에서 하나 열기(기본 정보 + 체크인 요약, 요청 2건)
//   - 채팅방 열기 (P_CHAT)     — 대화 목록에서 방 하나 들어가기(요청 1건)
//
// ★확률의 근거: 분당 기준으로 검색 1.2회 / 상세 0.8회 / 채팅 0.6회를 잡고 15초로 나눴다.
//   근거 있는 사용 로그가 없어 **가정**이다. 그래서 P_* 를 환경변수로 뺐다 —
//   가정을 바꿔 다시 돌릴 수 있어야 결론의 민감도를 볼 수 있다.
import http from 'k6/http';
import exec from 'k6/execution';
import { Trend, Rate, Counter } from 'k6/metrics';

const BASE = __ENV.BASE || 'http://localhost:8080';
const TOKENS = JSON.parse(open('./tokens.json'));

const LAT = 37.4971, LNG = 127.0276;   // 강남역 — 반경 1km에 영업 식당 2,914개(최악 케이스)

const P_SEARCH = Number(__ENV.P_SEARCH || 0.30);   // 15초당. 분당 1.2회
const P_DETAIL = Number(__ENV.P_DETAIL || 0.20);   // 분당 0.8회
const P_CHAT   = Number(__ENV.P_CHAT   || 0.15);   // 분당 0.6회

const TERMS2 = ['김밥', '치킨', '커피', '국수', '분식'];
const TERMS3 = ['파스타', '떡볶이', '삼겹살', '순대국', '곱창집'];

// 동시 접속자 단계. 폴링 단독이 300명에서 CPU 20%였으니 그 위를 훑는다.
// ★맨 뒤에 300을 한 번 더 둔다(대조군) — 1회차와 값이 같아야 워밍업·누적 오염이 없는 것이다.
const STEPS = (__ENV.USERS || '300,600,1000,1500,300').split(',').map(Number);
const DURATION_S = Number(__ENV.DURATION_S || 60);
const GAP = Number(__ENV.GAP_S || 20);
const WARMUP_S = Number(__ENV.WARMUP_S || 60);

// ★지표 이름은 사용자 수가 아니라 **단계 번호**로 만든다. 대조군 때문에 같은 사용자 수가
//   두 번 들어오는데, 이름을 사용자 수로 지으면 두 단계가 같은 지표에 더해져 대조군 비교가
//   불가능해진다(1차 실행에서 실제로 그렇게 나왔다 — 300명 두 줄이 똑같은 합계였다).
const key = (i) => `s${i}`;
const cycle = {}, fail = {}, done = {}, srch = {};
STEPS.forEach((n, i) => {
  cycle[key(i)] = new Trend(`cycle_${key(i)}`, true);   // 폴링 6개가 다 돌아오는 시간
  srch[key(i)]  = new Trend(`search_${key(i)}`, true);  // 검색 응답시간(섞였을 때)
  fail[key(i)]  = new Rate(`fail_${key(i)}`);
  done[key(i)]  = new Counter(`done_${key(i)}`);
});

const scenarios = {
  // ★워밍업은 가장 무거운 단계와 같은 강도로 준다. 약하게 주면 첫 단계가 JIT 비용을
  //   혼자 뒤집어써서 "사람이 늘수록 빨라지는" 거짓 결과가 나온다(08-24에 실제로 겪었다).
  warmup: {
    executor: 'constant-arrival-rate', exec: 'warmup',
    rate: Math.max(...STEPS), timeUnit: '15s', duration: `${WARMUP_S}s`,
    preAllocatedVUs: 60, maxVUs: 400, startTime: '0s', tags: { step: 'warmup' },
  },
};
STEPS.forEach((n, i) => {
  scenarios[key(i)] = {
    executor: 'constant-arrival-rate', exec: 'session',
    rate: n, timeUnit: '15s', duration: `${DURATION_S}s`,
    // 포화하면 한 주기가 수 초씩 걸려 VU가 급히 필요해진다. 모자라면 k6가 요청을 못 내보내고
    // dropped_iterations로 새는데, 그러면 서버가 버틴 건지 k6가 못 쏜 건지 구분이 안 된다.
    // ★넉넉히 예약한다. 6,000명 단계에서 200개만 예약했더니 k6가 VU를 제때 못 늘려
    //   21,074 iteration을 버렸고, 그게 "CPU 22%·실패율 96%"라는 가짜 결과로 나왔다.
    preAllocatedVUs: Math.min(n, 800), maxVUs: Math.max(n * 2, 800),
    startTime: `${WARMUP_S + GAP + i * (DURATION_S + GAP)}s`,
    env: { STEP: key(i) },
    tags: { step: key(i) },
  };
});

export const options = {
  scenarios,
  summaryTrendStats: ['avg', 'min', 'med', 'p(95)', 'p(99)', 'max'],
  // 임계값을 두지 않는다. 어디서 깨지는지 보는 테스트라 깨지는 게 정상이다.
};

const pick = (a) => a[Math.floor(Math.random() * a.length)];
const auth = (t) => ({ headers: { Authorization: `Bearer ${t}` }, timeout: '120s' });

// 앱이 15초마다 무조건 보내는 6개. MainTabs(conv·meal) + MapHome(stats·nearby·me) + 벨(unread).
function pollBatch(token) {
  const h = { headers: { Authorization: `Bearer ${token}` } };
  return [
    ['GET', `${BASE}/api/check-ins/stats`, null, h],
    ['GET', `${BASE}/api/places/nearby?lat=${LAT}&lng=${LNG}&radius=1000`, null, h],
    ['GET', `${BASE}/api/check-ins/me`, null, h],
    ['GET', `${BASE}/api/conversations`, null, h],
    ['GET', `${BASE}/api/meal-requests?role=received`, null, h],
    ['GET', `${BASE}/api/notifications/unread-count`, null, h],
  ];
}

export function warmup() {
  const t = TOKENS[exec.vu.idInTest % TOKENS.length];
  http.batch(pollBatch(t));
  http.get(`${BASE}/api/places/search?query=${encodeURIComponent(pick(TERMS2))}`, auth(t));
}

export function session() {
  const step = __ENV.STEP;
  const t = TOKENS[exec.vu.idInTest % TOKENS.length];
  const t0 = Date.now();
  let bad = 0, total = 0;

  // ① 폴링 — 항상
  for (const r of http.batch(pollBatch(t))) { total++; if (r.status !== 200) bad++; }
  cycle[step].add(Date.now() - t0);

  // ② 검색 세션 — 2글자로 훑고 3글자로 좁힌다(앱에서 타이핑하는 모양)
  if (Math.random() < P_SEARCH) {
    for (const q of [pick(TERMS2), pick(TERMS3)]) {
      const s0 = Date.now();
      const r = http.get(
        `${BASE}/api/places/search?query=${encodeURIComponent(q)}&lat=${LAT}&lng=${LNG}`, auth(t));
      srch[step].add(Date.now() - s0);
      total++; if (r.status !== 200) bad++;
    }
  }

  // ③ 식당 상세 — 검색 결과에서 하나 열기
  if (Math.random() < P_DETAIL) {
    const id = 100 + Math.floor(Math.random() * 5000);
    for (const u of [`/api/places/${id}`, `/api/places/${id}/checkin-summary`]) {
      const r = http.get(`${BASE}${u}`, auth(t));
      total++;
      // 404는 실패가 아니다 — 임의 id라 없는 식당을 고를 수 있다. 서버 오류만 센다.
      if (r.status >= 500) bad++;
    }
  }

  // ④ 채팅방 열기 — 목록에서 방 하나 들어가기
  if (Math.random() < P_CHAT) {
    const r = http.get(`${BASE}/api/conversations`, auth(t));
    total++; if (r.status !== 200) bad++;
  }

  fail[step].add(bad / total);
  done[step].add(1);
}

export function handleSummary(data) {
  const g = (n) => data.metrics[n] || { values: {} };
  const f = (x) => (Number.isFinite(x) ? x.toFixed(1).padStart(9) : '        -');
  const lines = [
    '',
    '동시 사용자별 결과  (1 iteration = 한 사용자의 15초 활동)',
    '  사용자  완료주기  요청/초   주기중앙   주기p95  검색중앙  검색p95   실패율',
  ];
  const dur = DURATION_S;
  STEPS.forEach((n, i) => {
    const k = key(i);
    const cnt = g(`done_${k}`).values.count || 0;
    const c = g(`cycle_${k}`).values, s = g(`search_${k}`).values;
    // 한 주기당 요청 수 = 폴링 6 + 검색 2·P + 상세 2·P + 채팅 1·P
    const perCycle = 6 + 2 * P_SEARCH + 2 * P_DETAIL + 1 * P_CHAT;
    lines.push(
      `${String(n).padStart(7)}${String(cnt).padStart(10)}` +
      `${(cnt * perCycle / dur).toFixed(1).padStart(9)}` +
      `${f(c.med)}${f(c['p(95)'])}${f(s.med)}${f(s['p(95)'])}` +
      `${((g(`fail_${k}`).values.rate || 0) * 100).toFixed(1).padStart(8)}%`);
  });
  lines.push('', `가정: 15초당 검색 ${P_SEARCH} · 상세 ${P_DETAIL} · 채팅 ${P_CHAT}`);

  // ★k6가 요청을 못 내보낸 경우를 크게 알린다. 이게 있으면 뒤 단계의 낮은 CPU·높은 실패율은
  //   "서버가 버틴 것"도 "서버가 죽은 것"도 아니고 **부하가 서버에 닿지 못한 것**이다.
  //   실제로 6,000명 단계에서 CPU 22%·실패율 96%가 나와 서버 한계로 오독할 뻔했다
  //   (VU를 4,400개까지밖에 못 만들고 21,074 iteration을 버렸다).
  const dropped = g('dropped_iterations').values.count || 0;
  const vmax = (data.metrics.vus_max || { values: {} }).values.max || 0;
  if (dropped > 0) {
    lines.push('',
      '★★ 측정 무효 구간이 있다 — k6가 요청을 다 내보내지 못했다.',
      `   버린 iteration ${dropped.toLocaleString()}건 · VU 최대 ${vmax.toLocaleString()}개`,
      '   VU가 모자란 단계의 숫자는 서버 성능이 아니라 k6·OS 한계다. 그 단계는 버려라.',
      '   대응: `ulimit -n 20000` 후 재실행하거나, 사용자 수 상한을 낮춰 잡는다.');
  }
  lines.push('');
  const out = lines.join('\n');
  const stamp = __ENV.STAMP || 'run';
  return { stdout: out, [`results/${stamp}-mixed-summary.txt`]: out,
           [`results/${stamp}-mixed.json`]: JSON.stringify(data) };
}
