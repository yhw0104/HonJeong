// 채팅 목록 N+1이 "처리량"에도 영향을 주는가.
//
// 단건 비용은 대화방당 0.03ms로 미미했다(0개 1.0ms → 50개 2.5ms). 그런데 이 API는
// MainTabs가 어느 탭에서든 15초마다 부른다 — 전 사용자가 상시 두들기는 엔드포인트다.
// 쿼리 수가 5개에서 55개로 11배가 되면 총량에서는 이야기가 달라질 수 있다.
//
// 같은 부하(초당 N건)를 대화 0개 집단과 50개 집단에 각각 주고 비교한다.
import http from 'k6/http';
import exec from 'k6/execution';
import { Trend, Rate, Counter } from 'k6/metrics';

const BASE = __ENV.BASE || 'http://localhost:8080';
const GROUPS = JSON.parse(open('./conv-groups.json'));
const RATE = Number(__ENV.RATE || 300);
const DURATION_S = Number(__ENV.DURATION_S || 30);
const SIZES = [0, 50];

const dur = {}, fail = {}, done = {};
for (const n of SIZES) {
  dur[`g${n}`] = new Trend(`dur_g${n}`, true);
  fail[`g${n}`] = new Rate(`fail_g${n}`);
  done[`g${n}`] = new Counter(`done_g${n}`);
}

const scenarios = {
  warmup: { executor: 'constant-arrival-rate', exec: 'warmup', rate: 30, timeUnit: '1s',
            duration: '30s', preAllocatedVUs: 20, maxVUs: 60, startTime: '0s' },
};
SIZES.forEach((n, i) => {
  scenarios[`g${n}`] = {
    executor: 'constant-arrival-rate', exec: 'list',
    rate: RATE, timeUnit: '1s', duration: `${DURATION_S}s`,
    preAllocatedVUs: 50, maxVUs: 400,
    startTime: `${30 + 15 + i * (DURATION_S + 15)}s`,
  };
});

export const options = { scenarios, summaryTrendStats: ['avg', 'med', 'p(95)', 'p(99)', 'max'] };

function hit(n, i) {
  const pool = GROUPS[String(n)];
  return http.get(`${BASE}/api/conversations`,
    { headers: { Authorization: `Bearer ${pool[i % pool.length]}` }, timeout: '60s' });
}
export function warmup() { for (const n of SIZES) hit(n, exec.scenario.iterationInTest); }
export function list() {
  const n = Number(exec.scenario.name.slice(1));
  const res = hit(n, exec.scenario.iterationInTest);
  dur[`g${n}`].add(res.timings.duration);
  fail[`g${n}`].add(res.status !== 200);
  done[`g${n}`].add(1);
}

export function handleSummary(data) {
  const g = (n, s) => (data.metrics[n] && data.metrics[n].values ? data.metrics[n].values[s] : NaN);
  const f = (v, w) => (Number.isFinite(v) ? v.toFixed(0).padStart(w) : '-'.padStart(w));
  const L = ['', `채팅 목록 처리량 비교 — 목표 초당 ${RATE}건`, '─'.repeat(66)];
  L.push('대화방  쿼리/건  완료   실제rate    중앙값     p(95)     최대');
  L.push('─'.repeat(66));
  for (const n of SIZES) {
    const c = g(`done_g${n}`, 'count');
    L.push(`${String(n).padStart(5)}개 ${String(n + 5).padStart(6)}개 ${String(c ?? '-').padStart(6)} ` +
      `${(Number.isFinite(c) ? (c / DURATION_S).toFixed(0) : '-').padStart(7)}/s ` +
      `${f(g(`dur_g${n}`, 'med'), 8)}ms ${f(g(`dur_g${n}`, 'p(95)'), 8)}ms ${f(g(`dur_g${n}`, 'max'), 7)}ms`);
  }
  L.push('─'.repeat(66));
  L.push('실제rate가 목표를 못 따라가면 그 지점이 서버가 못 버티는 곳이다.');
  L.push('');
  const out = { stdout: L.join('\n') };
  out[`results/${__ENV.STAMP || 'latest'}-conv-load.json`] = JSON.stringify(data, null, 2);
  return out;
}
