// 채팅 목록 N+1 측정 — 대화방 수에 따라 응답 시간이 어떻게 늘어나는가.
//
// ConversationService.listMine()은 미리보기는 배치로 한 번에 가져오는데(주석에 "N+1 제거"라고
// 적혀 있다) **안읽음 개수만 대화방마다 따로 센다**:
//
//     long unread = chatMessageRepository.countUnread(c.getId(), userId, c.lastReadAtFor(userId));
//
// DB 로그로 확인한 실제 쿼리 수 — 정확히 N에 비례한다:
//     대화 0개 → 4개 / 5개 → 10개 / 20개 → 25개 / 50개 → 55개
//
// ★사용자를 대화방 수로 나눠 둔 집단으로 잰다(seed-conversations.py). 같은 사용자에게 대화를
//   늘려가며 재면 캐시·계획 상태가 섞여 "대화가 늘어서 느린 건지 두 번째라 빠른 건지"를 가를 수 없다.
//
// ★VU 1개 순차. 이 테스트가 답할 질문은 "요청 하나가 얼마나 걸리나"이지 "몇 건까지 버티나"가
//   아니다. 동시 요청을 주면 경합이 섞여 N에 따른 증가분이 묻힌다.
import http from 'k6/http';
import exec from 'k6/execution';
import { Trend, Rate } from 'k6/metrics';

const BASE = __ENV.BASE || 'http://localhost:8080';
const GROUPS = JSON.parse(open('./conv-groups.json'));
const SIZES = Object.keys(GROUPS).map(Number).sort((a, b) => a - b);

const DURATION_S = Number(__ENV.DURATION_S || 30);
const GAP = Number(__ENV.GAP_S || 10);
const WARMUP_S = Number(__ENV.WARMUP_S || 30);

const dur = {}, fail = {};
for (const n of SIZES) {
  dur[`g${n}`] = new Trend(`dur_g${n}`, true);
  fail[`g${n}`] = new Rate(`fail_g${n}`);
}

const scenarios = {
  // 워밍업은 모든 그룹을 골고루 밟는다. 한 그룹만 예열하면 나머지가 첫 요청에서 JIT 비용을 문다.
  warmup: {
    executor: 'constant-arrival-rate', exec: 'warmup',
    rate: 20, timeUnit: '1s', duration: `${WARMUP_S}s`,
    preAllocatedVUs: 10, maxVUs: 40, startTime: '0s',
  },
};
SIZES.forEach((n, i) => {
  scenarios[`g${n}`] = {
    executor: 'constant-vus', exec: 'list', vus: 1,
    duration: `${DURATION_S}s`,
    startTime: `${WARMUP_S + GAP + i * (DURATION_S + GAP)}s`,
    tags: { size: String(n) },
  };
});

export const options = { scenarios, summaryTrendStats: ['avg', 'med', 'p(95)', 'max'] };

function hit(token) {
  return http.get(`${BASE}/api/conversations`, { headers: { Authorization: `Bearer ${token}` } });
}

export function warmup() {
  const n = SIZES[exec.scenario.iterationInTest % SIZES.length];
  const pool = GROUPS[String(n)];
  hit(pool[exec.scenario.iterationInTest % pool.length]);
}

export function list() {
  const n = Number(exec.scenario.name.slice(1));
  const pool = GROUPS[String(n)];
  // 반복마다 다른 사용자를 쓴다. 한 명만 두들기면 그 사용자의 캐시 상태만 재게 된다.
  const res = hit(pool[exec.scenario.iterationInTest % pool.length]);
  dur[`g${n}`].add(res.timings.duration);
  fail[`g${n}`].add(res.status !== 200);
}

export function handleSummary(data) {
  const g = (n, s) => (data.metrics[n] && data.metrics[n].values ? data.metrics[n].values[s] : NaN);
  const f = (v, w) => (Number.isFinite(v) ? v.toFixed(1).padStart(w) : '-'.padStart(w));

  const L = ['', '채팅 목록 — 대화방 수에 따른 응답 시간', '─'.repeat(68)];
  L.push('대화방  쿼리 수    평균     중앙값    p(95)   대화방당 비용   실패율');
  L.push('─'.repeat(68));
  const base = g('dur_g0', 'med');
  for (const n of SIZES) {
    const med = g(`dur_g${n}`, 'med');
    const per = n > 0 && Number.isFinite(med) && Number.isFinite(base) ? (med - base) / n : NaN;
    const fr = g(`fail_g${n}`, 'rate');
    L.push(`${String(n).padStart(5)}개 ${String(n + 5).padStart(6)}개 ${f(g(`dur_g${n}`, 'avg'), 8)}ms ` +
      `${f(med, 8)}ms ${f(g(`dur_g${n}`, 'p(95)'), 7)}ms ${n > 0 ? f(per, 10) + 'ms' : '         —'} ` +
      `${(Number.isFinite(fr) ? (fr * 100).toFixed(1) + '%' : '-').padStart(7)}`);
  }
  L.push('─'.repeat(68));
  L.push('쿼리 수는 DB 로그로 실측한 값이다(기본 5개 + 대화방당 1개).');
  L.push('"대화방당 비용"이 일정하면 N+1이 선형으로 비용을 더한다는 뜻이다.');
  L.push('');

  const out = { stdout: L.join('\n') };
  out[`results/${__ENV.STAMP || 'latest'}-conversations.json`] = JSON.stringify(data, null, 2);
  return out;
}
