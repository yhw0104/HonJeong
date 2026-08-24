// 테스트 3 — 폴링 부하. "홈 화면을 켜놓은 사용자 N명"이 서버에 주는 부하를 잰다.
//
// 목적: 가설 H4(= /places/nearby가 강남 좌표에서 엔티티 2,914개를 JVM에 올려 힙을 압박한다)를
// 검증한다. 검색과 달리 이건 사용자가 아무것도 안 해도 계속 흐르는 부하다.
//
// 왜 홈 탭인가: 앱에서 가장 무거운 화면이다(코드 전수 조사 결과).
//   MainTabs가 어느 탭에서든 conversations·meal-requests를 15초마다 돌리고,
//   MapHome이 stats·nearby·check-ins/me를, BellButton이 unread-count를 더한다 = 6개.
//   사용자 1명 = 분당 24요청.
//
// ★모든 가상 사용자를 강남역 좌표에 둔다. 최악 케이스를 의도적으로 만드는 것이다 —
//   강남역 반경 1km에 영업 중인 식당이 2,914개고, /places/nearby는 그 전부를 자바 메모리로
//   올린 뒤 haversine으로 거리를 계산하고 정렬해서 20개만 돌려준다(2,894개는 버린다).
//   한산한 좌표로 재면 이 비용이 안 드러난다.
//
// ★한 번의 iteration = 한 사용자의 "폴링 1주기"(6개 동시 요청)다. React Query가 화면 진입 시
//   6개를 한꺼번에 쏘므로 batch로 묶는다. 실제로는 각 쿼리의 15초 타이머가 서로 다른 시점에
//   시작해 조금씩 어긋나지만, constant-arrival-rate가 사용자들의 주기를 자연히 흩어 주므로
//   전체로 보면 실사용에 가깝다(사용자 1명 안에서만 6개가 동시에 나간다).
import http from 'k6/http';
import exec from 'k6/execution';
import { Trend, Rate, Counter } from 'k6/metrics';

const BASE = __ENV.BASE || 'http://localhost:8080';
const TOKENS = JSON.parse(open('./tokens.json'));

const LAT = 37.4971, LNG = 127.0276;   // 강남역

// 동시 접속자 수 단계. 300은 08-21 용량 추정에서 "한계"로 잡았던 숫자다.
// ★맨 뒤에 50을 한 번 더 둔다(대조군). 1차 실행에서 접속자가 늘수록 오히려 빨라지는
//   (50명 32ms → 300명 14ms) 결과가 나왔는데, 부하 때문일 리 없고 JIT 워밍업이 부족해
//   뒤 단계일수록 예열된 상태였기 때문이다. 마지막에 첫 단계를 되풀이해서,
//   같은 부하가 같은 숫자를 내는지로 그 오염이 남았는지 판정한다.
//   1회차 50명 ≈ 2회차 50명이면 워밍업이 충분한 것이다.
const STEPS = (__ENV.USERS || '50,100,200,300,50').split(',').map(Number);
const DURATION_S = Number(__ENV.DURATION_S || 60);
const GAP = Number(__ENV.GAP_S || 20);      // 앞 단계의 대기열·GC가 가라앉을 시간
// ★워밍업은 가장 무거운 단계와 같은 강도로 준다. 1차 실행의 워밍업은 20주기/15초(=1.33/초)로
//   30초, 즉 40주기뿐이었다 — JIT 컴파일이 끝나기엔 한참 모자랐다.
const WARMUP_S = Number(__ENV.WARMUP_S || 60);
const WARMUP_USERS = Number(__ENV.WARMUP_USERS || 300);
const POLL_S = 15;                           // LIVE_REFETCH_MS = 15초 (shared/realtime.ts)

// 홈 탭이 15초마다 부르는 6개. 이름은 결과표에 그대로 쓴다.
const ENDPOINTS = [
  ['stats',  '/api/check-ins/stats'],
  ['nearby', `/api/places/nearby?lat=${LAT}&lng=${LNG}&radius=1000`],
  ['me',     '/api/check-ins/me'],
  ['conv',   '/api/conversations'],
  ['meal',   '/api/meal-requests?role=received'],
  ['unread', '/api/notifications/unread-count'],
];

const key = (n, i) => `s${i}u${n}`;   // 같은 n이 두 번 나와도 시나리오 키가 겹치지 않게
const cycle = {}, fail = {}, done = {};
const perEp = {};
STEPS.forEach((n, i) => {
  const k = key(n, i);
  cycle[k] = new Trend(`cycle_${k}`, true);   // 6개가 다 돌아오기까지
  fail[k] = new Rate(`fail_${k}`);
  done[k] = new Counter(`done_${k}`);
  for (const [ep] of ENDPOINTS) perEp[`${k}_${ep}`] = new Trend(`ep_${k}_${ep}`, true);
});

const scenarios = {
  warmup: {
    executor: 'constant-arrival-rate',
    exec: 'warmup', rate: WARMUP_USERS, timeUnit: `${POLL_S}s`, duration: `${WARMUP_S}s`,
    preAllocatedVUs: 40, maxVUs: 200, startTime: '0s',
  },
};
STEPS.forEach((n, i) => {
  scenarios[key(n, i)] = {
    executor: 'constant-arrival-rate',
    exec: 'poll',
    // ★rate를 "15초당 N주기"로 준다 = 동시 접속자 N명. 초당으로 환산하면 N/15 주기/초이고,
    //   요청 수로는 N/15*6 건/초다(300명이면 초당 120요청).
    rate: n, timeUnit: `${POLL_S}s`,
    duration: `${DURATION_S}s`,
    // 한 주기가 6개 동시 요청이라 VU 하나가 오래 붙잡힌다. 포화하면 급격히 더 필요해진다.
    preAllocatedVUs: Math.ceil(n / 4), maxVUs: n * 2,
    startTime: `${WARMUP_S + GAP + i * (DURATION_S + GAP)}s`,
    tags: { step: key(n, i) },
  };
});

export const options = { scenarios, summaryTrendStats: ['avg', 'med', 'p(95)', 'p(99)', 'max'] };

function batchFor(token) {
  const headers = { Authorization: `Bearer ${token}` };
  return ENDPOINTS.map(([, path]) => ['GET', `${BASE}${path}`, null, { headers }]);
}

export function warmup() {
  http.batch(batchFor(TOKENS[exec.vu.idInTest % TOKENS.length]));
}

export function poll() {
  const k = exec.scenario.name;
  const token = TOKENS[exec.vu.idInTest % TOKENS.length];
  const t0 = Date.now();
  const res = http.batch(batchFor(token));
  cycle[k].add(Date.now() - t0);
  done[k].add(1);

  let bad = 0;
  res.forEach((r, i) => {
    perEp[`${k}_${ENDPOINTS[i][0]}`].add(r.timings.duration);
    if (r.status !== 200) bad++;
  });
  fail[k].add(bad > 0);
}

export function handleSummary(data) {
  const g = (n, s) => (data.metrics[n] && data.metrics[n].values ? data.metrics[n].values[s] : NaN);
  const f = (n, w) => (Number.isFinite(n) ? n.toFixed(0).padStart(w) : '-'.padStart(w));

  const L = [];
  L.push('');
  L.push('폴링 부하 — 홈 탭, 전원 강남역 좌표, 15초 주기');
  L.push('─'.repeat(78));
  L.push('접속자  요청/초  주기완료  주기 중앙값   주기 p95   실패율');
  L.push('─'.repeat(78));
  STEPS.forEach((n, i) => {
    const k = key(n, i);
    const c = g(`done_${k}`, 'count');
    const fr = g(`fail_${k}`, 'rate');
    const mark = STEPS.indexOf(n) !== i ? '↺' : ' ';   // 되풀이 단계 표시
    L.push(
      `${mark}${String(n).padStart(4)}명 ${(n / POLL_S * ENDPOINTS.length).toFixed(0).padStart(7)} ` +
      `${String(Number.isFinite(c) ? c : '-').padStart(9)} ${f(g(`cycle_${k}`, 'med'), 11)}ms ` +
      `${f(g(`cycle_${k}`, 'p(95)'), 9)}ms ${(Number.isFinite(fr) ? (fr * 100).toFixed(1) + '%' : '-').padStart(8)}`
    );
  });
  L.push('─'.repeat(78));
  L.push('');
  L.push('엔드포인트별 중앙값 (ms) — nearby가 홀로 튀면 가설 H4가 맞은 것이다');
  L.push('─'.repeat(78));
  L.push('접속자 ' + ENDPOINTS.map(([e]) => e.padStart(9)).join(''));
  STEPS.forEach((n, i) => {
    const mark = STEPS.indexOf(n) !== i ? '↺' : ' ';
    L.push(mark + String(n).padStart(4) + '명 ' +
      ENDPOINTS.map(([e]) => f(g(`ep_${key(n, i)}_${e}`, 'med'), 9)).join(''));
  });
  L.push('─'.repeat(78));
  L.push('↺ = 첫 단계 되풀이(대조군). 1회차와 값이 비슷해야 워밍업이 충분했던 것이다.');
  const dropped = g('dropped_iterations', 'count');
  if (Number.isFinite(dropped) && dropped > 0) L.push(`★ k6가 못 내보낸 주기 ${dropped}건 (VU 부족 가능성)`);
  L.push('');

  const out = {};
  out['stdout'] = L.join('\n');
  out[`results/${__ENV.STAMP || 'latest'}-polling.json`] = JSON.stringify(data, null, 2);
  return out;
}
