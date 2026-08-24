// 테스트 1 — 검색 단독 램프업. 2글자 검색어만으로 포화점을 찾는다.
//
// 목적: "고치기 전 이 서버는 초당 몇 건의 검색을 감당하는가"를 숫자로 박는다.
// 개선(ORDER BY 처리·Page→Slice·MIN_SEARCH_LEN)을 적용한 뒤 같은 스크립트를 다시 돌려
// before/after를 만든다.
//
// 조건(2026-08-24):
//   - app·db를 **같은 코어 2개**에 묶었다(cpuset 0,1). 운영 Lightsail 2vCPU를 흉내낸 것으로,
//     이걸 안 하면 postgres 병렬 스캔이 맥 10코어를 다 끌어써서 맥의 한계를 재게 된다.
//   - 그 조건에서 2글자 좌표없음 단건 = 660ms. 대부분 DB에서 쓴다(목록 424ms + COUNT 298ms).
//   - 660ms가 거의 CPU 시간이면 코어 1개당 ~1.5건/초, 2코어면 ~3건/초가 이론 상한이다.
//     그래서 0.5 → 6건/초를 훑어 그 부근을 감싼다.
//
// ★단계를 ramping-arrival-rate 하나로 잇지 않고 constant-arrival-rate 6개를 순차로 돌린다.
//   연속 램프는 구간 경계가 뭉개져서 "어느 rate에서 p95가 얼마였나"를 사후에 못 가른다.
//   단계마다 별도 시나리오면 k6가 알아서 분리해 준다.
import http from 'k6/http';
import exec from 'k6/execution';
import { Trend, Rate, Counter } from 'k6/metrics';

const BASE = __ENV.BASE || 'http://localhost:8080';
const TOKENS = JSON.parse(open('./tokens.json'));

// 2글자만. 매칭 건수를 5,731~20,964건으로 흩어 놓았다 — 2글자는 전체 스캔이라 건수와
// 무관하게 비용이 평평하다는 걸 이 테스트에서도 재확인할 수 있다.
const TERMS = ['김밥', '치킨', '커피', '국수', '분식'];

// 좌표없음 경로로 고정한다. 좌표있음은 준비 실측에서 34~145ms로 4배 넘게 흔들렸다 —
// 준비된 구문(prepared statement)의 커스텀/제네릭 계획 선택이 재시작·실행횟수에 따라
// 뒤집히기 때문이다. 포화점을 재는 테스트에 그런 불안정을 섞으면 knee가 계획 변덕인지
// 부하 때문인지 구분할 수 없다.
const RATES = (__ENV.RATES || '0.5,1,2,3,4,6').split(',').map(Number);
const DURATION_S = Number(__ENV.DURATION_S || 30);
const GAP = Number(__ENV.GAP_S || 15);   // 앞 단계의 대기열이 빠질 시간을 준다
const WARMUP_S = Number(__ENV.WARMUP_S || 20);

const key = (r) => `r${String(r).replace('.', '_')}`;

const dur = {}, fail = {}, done = {};
for (const r of RATES) {
  dur[key(r)] = new Trend(`dur_${key(r)}`, true);
  fail[key(r)] = new Rate(`fail_${key(r)}`);
  done[key(r)] = new Counter(`done_${key(r)}`);
}

const scenarios = {
  warmup: {
    executor: 'constant-arrival-rate',
    exec: 'warmup', rate: 2, timeUnit: '1s', duration: `${WARMUP_S}s`,
    preAllocatedVUs: 10, maxVUs: 30, startTime: '0s', tags: { step: 'warmup' },
  },
};
RATES.forEach((r, i) => {
  scenarios[key(r)] = {
    executor: 'constant-arrival-rate',
    exec: 'search',
    rate: r * 10, timeUnit: '10s',        // 0.5건/초 같은 소수를 정수로 표현하기 위해 10초 단위
    duration: `${DURATION_S}s`,
    // ★maxVUs를 넉넉히 준다. 포화하면 한 요청이 수 초씩 걸려 VU가 급격히 필요해지는데,
    //   모자라면 k6가 요청을 아예 못 내보내고 dropped_iterations로 샌다 — 그러면 서버가
    //   버틴 건지 k6가 못 쏜 건지 구분이 안 된다.
    preAllocatedVUs: 10, maxVUs: 200,
    startTime: `${WARMUP_S + GAP + i * (DURATION_S + GAP)}s`,
    tags: { step: key(r) },
  };
});

export const options = {
  scenarios,
  summaryTrendStats: ['avg', 'min', 'med', 'p(95)', 'p(99)', 'max'],
  // 임계값을 두지 않는다. 이 테스트는 "어디서 깨지나"를 보는 것이라 깨지는 게 정상이다.
};

function hit(q) {
  const token = TOKENS[exec.vu.idInTest % TOKENS.length];
  return http.get(`${BASE}/api/places/search?query=${encodeURIComponent(q)}`,
    { headers: { Authorization: `Bearer ${token}` }, timeout: '120s' });
}

export function warmup() {
  hit(TERMS[exec.scenario.iterationInTest % TERMS.length]);
}

export function search() {
  const k = exec.scenario.name;
  const res = hit(TERMS[exec.scenario.iterationInTest % TERMS.length]);
  dur[k].add(res.timings.duration);
  fail[k].add(res.status !== 200);
  done[k].add(1);
}

export function handleSummary(data) {
  const g = (n, s) => (data.metrics[n] && data.metrics[n].values ? data.metrics[n].values[s] : NaN);
  const f = (n, w = 8) => (Number.isFinite(n) ? n.toFixed(0).padStart(w) : '-'.padStart(w));

  const L = [];
  L.push('');
  L.push('검색 램프업 — 2글자 검색어, 좌표없음');
  L.push('─'.repeat(72));
  L.push('목표rate   완료   실제rate    중앙값     p(95)      최대   실패율');
  L.push('─'.repeat(72));
  for (const r of RATES) {
    const k = key(r);
    const n = g(`done_${k}`, 'count');
    const actual = Number.isFinite(n) ? n / DURATION_S : NaN;
    const fr = g(`fail_${k}`, 'rate');
    L.push(
      `${String(r).padStart(6)}/s ${String(Number.isFinite(n) ? n : '-').padStart(6)} ` +
      `${(Number.isFinite(actual) ? actual.toFixed(2) : '-').padStart(9)}/s ` +
      `${f(g(`dur_${k}`, 'med'))}ms ${f(g(`dur_${k}`, 'p(95)'))}ms ${f(g(`dur_${k}`, 'max'))}ms ` +
      `${(Number.isFinite(fr) ? (fr * 100).toFixed(1) + '%' : '-').padStart(7)}`
    );
  }
  L.push('─'.repeat(72));
  const dropped = g('dropped_iterations', 'count');
  if (Number.isFinite(dropped) && dropped > 0) {
    L.push(`★ k6가 못 내보낸 요청 ${dropped}건 — VU 부족이다. maxVUs를 올려 다시 잴 것.`);
  }
  L.push('읽는 법: 목표rate와 실제rate가 벌어지기 시작하는 지점이 서버가 못 따라오는 지점이다.');
  L.push('        중앙값이 계단처럼 뛰면 대기열이 쌓이기 시작한 것이다.');
  L.push('');

  const out = {};
  out['stdout'] = L.join('\n');
  out[`results/${__ENV.STAMP || 'latest'}-search-ramp.json`] = JSON.stringify(data, null, 2);
  return out;
}
