// 테스트 2 — 검색어 길이 비교.
//
// 가설: /places/search가 느린 원인은 "검색량"이 아니라 **검색어 길이가 옵티마이저의 실행계획을
// 바꾸는 것**이다. 2글자는 trigram 선택도 추정이 빗나가 전체 스캔으로 떨어지고, 3글자부터는
// 이름 인덱스를 탄다.
//
// 사전 DB 실측(EXPLAIN ANALYZE, 병렬 워커 끔):
//   2글자: 전부 Seq Scan, 113~124ms — 매칭 건수가 5,731~20,964건으로 3.7배 차이나도 시간은 평평.
//          전체 테이블(655,163행)을 훑기 때문에 건수와 무관하다.
//   3~4글자: 전부 Bitmap Index Scan, 0.1~8.2ms — 매칭 건수에 비례한다.
// 즉 길이가 "계획"을 정하고, 계획이 "비용의 종류"를 정한다. 이 스크립트는 그게 HTTP 응답시간까지
// 올라오는지를 확인한다.
//
// 좌표 유무 두 경로를 모두 잰다. 서로 다른 이유로 느려지므로 하나만 재면 결론이 반쪽이 된다:
//   좌표없음 → Page<>의 COUNT 쿼리가 전체 스캔 (2글자 ~121ms)
//   좌표있음 → COUNT는 없지만 2글자면 좌표 인덱스로 77,014엔트리를 훑고 이름으로 거른다 (~25ms)
//
// 실행: ./run-search-length.sh   (docker stats 동시 수집까지 해준다)
import http from 'k6/http';
import exec from 'k6/execution';
import { check } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const BASE = __ENV.BASE || 'http://localhost:8080';
const TOKENS = JSON.parse(open('./tokens.json'));

// 강남역. nearby 측정에 쓴 좌표와 같게 둬서 비교가 가능하게 한다.
const LAT = 37.4971;
const LNG = 127.0276;

// 각 그룹 안에서 매칭 건수를 일부러 넓게 흩어 놓았다 — 건수가 아니라 길이가 원인이라는 걸
// 결과가 스스로 보이게 하기 위해서다(2글자 그룹은 건수가 흩어져도 시간이 평평해야 한다).
const TERMS = {
  len2: ['김밥', '치킨', '커피', '국수', '분식'],            // 5,731 ~ 20,964건
  len3: ['파스타', '떡볶이', '삼겹살', '순대국', '곱창집'],   //    47 ~  4,290건
  len4: ['스타벅스', '김밥천국', '삼겹살집', '파스타집'],      //    17 ~  1,112건
};

const VARIANTS = [
  'len2_nocoord', 'len3_nocoord', 'len4_nocoord',
  'len2_coord', 'len3_coord', 'len4_coord',
];

// 변형별 응답시간을 따로 모은다. 태그만으로는 기본 요약에 p95가 변형별로 안 나온다.
const dur = {};
const fail = {};
for (const v of VARIANTS) {
  dur[v] = new Trend(`dur_${v}`, true);
  fail[v] = new Rate(`fail_${v}`);
}

// ★변형을 순차 실행한다(동시 실행 금지). 2코어를 나눠 쓰면 서로의 응답시간을 오염시켜
//   "길이 차이"가 아니라 "경합 차이"를 재게 된다.
// ★DURATION은 초 단위 숫자로 받는다. 문자열('30s')로 받으면 아래 startTime 계산에 못 쓰고,
//   그러면 duration만 바꿨을 때 변형들이 겹쳐 실행돼 순차 실행이라는 전제가 조용히 깨진다.
const DURATION_S = Number(__ENV.DURATION_S || 30);
const GAP = Number(__ENV.GAP_S || 10);   // 변형 사이 쉬는 시간(초) — GC·캐시 상태를 가라앉힌다
const STEP = DURATION_S + GAP;

// ★워밍업이 없으면 맨 앞 변형이 JVM의 JIT 컴파일·클래스 로딩·커넥션 풀 예열 비용을 전부
//   뒤집어쓴다. 스모크 런에서 len2_nocoord 중앙값이 650ms로 나왔는데, 같은 요청을 curl로 예열
//   후 재면 126ms였다 — 5배가 순전히 실행 순서 때문이었다. 순서가 결과를 만들면 그건 측정이 아니다.
const WARMUP_S = Number(__ENV.WARMUP_S || 20);

// ★constant-arrival-rate(초당 N건)로 재면 변형끼리 대기열 길이가 달라져 응답시간에 경합이
//   섞인다. 실제로 RATE=5로 돌렸을 때 2글자 좌표없음이 877ms가 나왔는데, 같은 요청 단건은
//   ~340ms였다 — 차액 대부분이 DB 경합이었다. 길이 비교는 "요청 하나가 얼마나 걸리나"를
//   재는 것이므로 VU 1개로 순차 실행해 경합을 0으로 만든다.
//   포화점·처리량은 별도 테스트(테스트 1, 검색 단독 램프업)의 몫이다.
const VUS = Number(__ENV.VUS || 1);

const scenarios = {
  warmup: {
    executor: 'constant-arrival-rate',
    exec: 'warmup',
    rate: 10,
    timeUnit: '1s',
    duration: `${WARMUP_S}s`,
    preAllocatedVUs: 10,
    maxVUs: 30,
    startTime: '0s',
    tags: { variant: 'warmup' },   // 측정 대상이 아니다 — 아래 Trend에 넣지 않는다
  },
};
VARIANTS.forEach((v, i) => {
  scenarios[v] = {
    executor: 'constant-vus',
    exec: 'search',
    vus: VUS,
    duration: `${DURATION_S}s`,
    startTime: `${WARMUP_S + GAP + i * STEP}s`,
    tags: { variant: v },
  };
});

export const options = {
  scenarios,
  // 이 테스트는 저부하라 실패가 나오면 안 된다. 하나라도 나면 측정이 아니라 고장이다.
  // 워밍업 요청은 임계값 판정에서 뺀다 — 예열 중 느린 건 고장이 아니다.
  thresholds: { 'http_req_failed{variant:!warmup}': ['rate<0.01'] },
  summaryTrendStats: ['avg', 'min', 'med', 'p(95)', 'p(99)', 'max'],
};

// 6개 변형이 쓰는 경로를 골고루 한 번씩 밟아 둔다. 한 경로만 예열하면 나머지가 첫 요청에서
// 자기 몫의 JIT 비용을 치른다(계획이 갈리는 두 경로는 코드 경로 자체가 다르다).
export function warmup() {
  const token = TOKENS[exec.vu.idInTest % TOKENS.length];
  const headers = { Authorization: `Bearer ${token}` };
  const i = exec.scenario.iterationInTest;
  for (const lenKey of ['len2', 'len3', 'len4']) {
    const terms = TERMS[lenKey];
    const q = encodeURIComponent(terms[i % terms.length]);
    http.get(`${BASE}/api/places/search?query=${q}`, { headers });
    http.get(`${BASE}/api/places/search?query=${q}&lat=${LAT}&lng=${LNG}`, { headers });
  }
}

export function search() {
  const v = exec.scenario.name;
  const [lenKey, coordMode] = v.split('_');
  const terms = TERMS[lenKey];
  // 매 반복 다른 검색어를 쓴다. 한 단어만 반복하면 그 단어의 실행계획·캐시 상태만 재게 된다.
  const q = terms[exec.scenario.iterationInTest % terms.length];

  let url = `${BASE}/api/places/search?query=${encodeURIComponent(q)}`;
  if (coordMode === 'coord') url += `&lat=${LAT}&lng=${LNG}`;

  const token = TOKENS[exec.vu.idInTest % TOKENS.length];
  const res = http.get(url, {
    headers: { Authorization: `Bearer ${token}` },
    tags: { variant: v },
  });

  dur[v].add(res.timings.duration);
  fail[v].add(res.status !== 200);
  check(res, { '200': (r) => r.status === 200 });
}

// 기본 요약은 변형 6개가 섞여 나와 비교가 안 된다. 필요한 표만 직접 만든다.
// (jslib의 textSummary를 쓰면 https 임포트가 필요해 네트워크에 의존하게 되므로 쓰지 않는다.)
export function handleSummary(data) {
  const g = (name, stat) => {
    const m = data.metrics[name];
    return m && m.values ? m.values[stat] : NaN;
  };
  const f = (n) => (Number.isFinite(n) ? n.toFixed(1).padStart(7) : '      -');

  const lines = [];
  lines.push('');
  lines.push('검색어 길이별 응답시간 (ms)');
  lines.push('─'.repeat(64));
  lines.push('경로          길이      평균    중앙값   p(95)   실패율');
  lines.push('─'.repeat(64));

  const rows = [
    ['좌표없음', 'len2_nocoord', '2글자'], ['좌표없음', 'len3_nocoord', '3글자'], ['좌표없음', 'len4_nocoord', '4글자'],
    ['좌표있음', 'len2_coord', '2글자'], ['좌표있음', 'len3_coord', '3글자'], ['좌표있음', 'len4_coord', '4글자'],
  ];
  let prev = '';
  for (const [path, key, len] of rows) {
    const label = path === prev ? '        ' : path.padEnd(8);
    prev = path;
    const fr = g(`fail_${key}`, 'rate');
    lines.push(`${label}    ${len}  ${f(g(`dur_${key}`, 'avg'))} ${f(g(`dur_${key}`, 'med'))} ${f(g(`dur_${key}`, 'p(95)'))}   ${Number.isFinite(fr) ? (fr * 100).toFixed(1) + '%' : '-'}`);
  }
  lines.push('─'.repeat(64));

  // 이 테스트의 결론 한 줄. 배수는 중앙값으로 낸다 — p95는 GC 한 번에 흔들린다.
  const ratio = (a, b) => {
    const x = g(`dur_${a}`, 'med'), y = g(`dur_${b}`, 'med');
    return Number.isFinite(x) && Number.isFinite(y) && y > 0 ? (x / y).toFixed(1) : '?';
  };
  lines.push(`2글자 대비 3글자 개선폭 — 좌표없음 ${ratio('len2_nocoord', 'len3_nocoord')}배 · 좌표있음 ${ratio('len2_coord', 'len3_coord')}배`);
  lines.push('');

  const stamp = __ENV.STAMP || 'latest';
  const out = {};
  out['stdout'] = lines.join('\n');
  out[`results/${stamp}-search-length.json`] = JSON.stringify(data, null, 2);
  return out;
}
