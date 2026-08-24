// 테스트 4 — 회복 테스트. 부하를 걷어내면 서버가 스스로 돌아오는가?
//
// 가설 H5: app/App.tsx:19의 `retry: 1` 때문에 못 돌아온다.
//   요청 실패 → 앱이 자동 1회 재시도 → 트래픽 2배 → 더 느려짐 → 더 실패 → ...
//   이걸 메타안정 장애(metastable failure)라 한다. 부하를 원래대로 낮춰도 시스템이
//   스스로 회복하지 못하는 상태다. "300명에서 꺾였는데 100명으로 낮춰도 안 돌아온다"가
//   나오면 범인은 서버 용량이 아니라 클라이언트의 재시도다.
//
// 구조 — 3구간을 한 시나리오 안에서 이어 붙인다(ramping-arrival-rate):
//   ① 평상시  BASE_RATE 건/초 × BASE_S초    ← 회복 후와 비교할 기준선
//   ② 과부하  PEAK_RATE 건/초 × PEAK_S초    ← 포화점(테스트 1에서 4건/초) 위로 밀어붙인다
//   ③ 복귀    다시 BASE_RATE × RECOVER_S초  ← ①과 같은 부하. 응답시간이 ①로 돌아오는가?
//
// ★결론은 ①과 ③의 비교로만 난다. ③이 ①보다 계속 느리면 회복 실패다.
//
// ★RETRY=1로 주면 앱과 같은 재시도(1회)를 흉내낸다. 두 번 돌려 비교하는 게 이 테스트의 핵심이다:
//     RETRY=0 → 재시도 없음. 서버 자체의 회복력
//     RETRY=1 → 앱과 동일. 재시도가 회복을 막는지
//   차이가 없으면 H5는 기각이고, 그것도 기록할 값어치가 있는 결과다.
import http from 'k6/http';
import exec from 'k6/execution';
import { Trend, Rate, Counter } from 'k6/metrics';

const BASE = __ENV.BASE || 'http://localhost:8080';
const TOKENS = JSON.parse(open('./tokens.json'));

// 테스트 1에서 확인한 값: 3건/초까지 정상, 4건/초에서 붕괴, 상한 4.4건/초.
// 평상시는 여유롭게, 과부하는 확실히 넘기게 잡는다.
const BASE_RATE = Number(__ENV.BASE_RATE || 2);
const PEAK_RATE = Number(__ENV.PEAK_RATE || 8);
const BASE_S = Number(__ENV.BASE_S || 60);
// ★과부하 길이는 "밀린 요청이 다 빠지고도 관찰할 시간이 남도록" 역산해서 정한다.
//   처리량 상한이 4.4건/초(테스트 1)이므로 8건/초로 T초를 밀면 (8-4.4)*T 건이 밀리고,
//   복귀 후 (4.4-2)=2.4건/초로 빠지므로 배수에 (8-4.4)*T/2.4 초가 걸린다.
//     과부하 90초 → 밀린 324건 → 배수 135초  ← 복귀 150초가 통째로 배수라 관찰 구간이 없다
//     과부하 60초 → 밀린 216건 → 배수  90초  ← 복귀 180초 중 뒤 90초는 배수가 끝난 상태
//   1차 실행(90초/150초)이 딱 앞의 경우여서, "회복 실패 17.79배"가 진짜 회복 불능인지
//   그냥 큐가 빠지는 중이었는지 구분할 수 없었다.
const PEAK_S = Number(__ENV.PEAK_S || 60);
const RECOVER_S = Number(__ENV.RECOVER_S || 180);
const RETRY = __ENV.RETRY === '1';

// 테스트 1과 같은 부하원을 쓴다(2글자·좌표없음). 비교 가능성을 위해 바꾸지 않는다.
const TERMS = ['김밥', '치킨', '커피', '국수', '분식'];

const phaseDur = new Trend('dur_by_phase', true);
const p = {
  baseline: new Trend('dur_baseline', true),
  peak: new Trend('dur_peak', true),
  recover: new Trend('dur_recover', true),
  // ★복귀 구간의 마지막 TAIL_S초만 따로 모은다. 이게 진짜 판정 지표다 —
  //   앞부분은 밀린 요청을 처리하는 중이라 느린 게 당연하고, 뒷부분이 ①로 돌아왔는지가
  //   "스스로 회복하는가"의 답이다. 전체 평균으로 보면 배수 시간에 가려 답이 안 보인다.
  recoverTail: new Trend('dur_recover_tail', true),
};
const pFail = {
  baseline: new Rate('fail_baseline'), peak: new Rate('fail_peak'),
  recover: new Rate('fail_recover'), recoverTail: new Rate('fail_recover_tail'),
};
const retries = new Counter('retry_sent');
const reqTotal = new Counter('req_total');

export const options = {
  scenarios: {
    // 워밍업을 따로 두지 않는다 — ①구간 자체가 60초라 앞부분 몇 초의 JIT 비용은
    // 중앙값에 거의 영향이 없고, 오히려 ①과 ③의 조건을 똑같이 맞추는 게 중요하다.
    // (워밍업을 넣으면 ①만 예열된 상태가 되어 ③과의 비교가 기울어진다.)
    phases: {
      executor: 'ramping-arrival-rate',
      exec: 'hit',
      startRate: BASE_RATE, timeUnit: '1s',
      preAllocatedVUs: 20, maxVUs: 400,
      stages: [
        { target: BASE_RATE, duration: `${BASE_S}s` },   // ① 평상시
        { target: PEAK_RATE, duration: '5s' },           // 급격히 올린다(서서히 올리면 적응해 버린다)
        { target: PEAK_RATE, duration: `${PEAK_S}s` },   // ② 과부하
        { target: BASE_RATE, duration: '5s' },           // 급격히 내린다
        { target: BASE_RATE, duration: `${RECOVER_S}s` },// ③ 복귀 (뒤 TAIL_S초가 판정 구간)
      ],
    },
  },
  summaryTrendStats: ['avg', 'med', 'p(95)', 'p(99)', 'max'],
};

const T_PEAK_START = BASE_S;
const T_PEAK_END = BASE_S + 5 + PEAK_S;
const T_RECOVER_START = T_PEAK_END + 5;
const TAIL_S = Number(__ENV.TAIL_S || 60);
const T_TAIL_START = T_RECOVER_START + RECOVER_S - TAIL_S;

function phaseOf(elapsedS) {
  if (elapsedS < T_PEAK_START) return 'baseline';
  if (elapsedS < T_RECOVER_START) return 'peak';
  return 'recover';
}

export function hit() {
  const token = TOKENS[exec.vu.idInTest % TOKENS.length];
  const q = TERMS[exec.scenario.iterationInTest % TERMS.length];
  const url = `${BASE}/api/places/search?query=${encodeURIComponent(q)}`;
  const opts = { headers: { Authorization: `Bearer ${token}` }, timeout: '60s' };

  // ★progress(0~1)에 총시간을 곱하는 방식은 쓰지 않는다. 구간 판정이 어긋나면 ①과 ③이
  //   뒤섞여 결과 전체가 무의미해지는데, progress의 갱신 시점은 executor 구현에 달려 있어
  //   경계에서 어떻게 반올림되는지 보장이 없다. 시나리오 시작 시각으로부터의 실경과시간을 쓴다.
  const phase = phaseOf((Date.now() - exec.scenario.startTime) / 1000);

  let res = http.get(url, opts);
  reqTotal.add(1);
  let total = res.timings.duration;

  // ★앱의 retry: 1 재현. 실패했을 때만 한 번 더 보낸다. 사용자가 체감하는 시간은 두 번의 합이므로
  //   응답시간도 더해서 기록한다 — 재시도가 "숨겨주는" 게 아니라 "늘리는" 것임을 드러내기 위함이다.
  if (RETRY && res.status !== 200) {
    retries.add(1);
    res = http.get(url, opts);
    reqTotal.add(1);
    total += res.timings.duration;
  }

  p[phase].add(total);
  pFail[phase].add(res.status !== 200);
  phaseDur.add(total, { phase });
  // 복귀 구간의 꼬리는 recover에도 넣고 별도로도 넣는다(이중 집계가 아니라 부분집합이다).
  if (phase === 'recover' && (Date.now() - exec.scenario.startTime) / 1000 >= T_TAIL_START) {
    p.recoverTail.add(total);
    pFail.recoverTail.add(res.status !== 200);
  }
}

export function handleSummary(data) {
  const g = (n, s) => (data.metrics[n] && data.metrics[n].values ? data.metrics[n].values[s] : NaN);
  const f = (n, w) => (Number.isFinite(n) ? n.toFixed(0).padStart(w) : '-'.padStart(w));

  const L = [];
  L.push('');
  L.push(`회복 테스트 — 재시도 ${RETRY ? 'ON (앱의 retry:1 재현)' : 'OFF'}`);
  L.push(`  ① 평상시 ${BASE_RATE}/s × ${BASE_S}s  →  ② 과부하 ${PEAK_RATE}/s × ${PEAK_S}s  →  ③ 복귀 ${BASE_RATE}/s × ${RECOVER_S}s`);
  L.push('─'.repeat(66));
  L.push('구간          중앙값      p(95)       최대   실패율');
  L.push('─'.repeat(66));
  for (const [k, label] of [['baseline', '① 평상시'], ['peak', '② 과부하'],
                            ['recover', '③ 복귀 전체'], ['recover_tail', `③ 복귀 뒤${TAIL_S}s`]]) {
    const fr = g(`fail_${k}`, 'rate');
    L.push(`${label}  ${f(g(`dur_${k}`, 'med'), 9)}ms ${f(g(`dur_${k}`, 'p(95)'), 9)}ms ${f(g(`dur_${k}`, 'max'), 9)}ms ` +
      `${(Number.isFinite(fr) ? (fr * 100).toFixed(1) + '%' : '-').padStart(7)}`);
  }
  L.push('─'.repeat(66));

  const b = g('dur_baseline', 'med');
  const rAll = g('dur_recover', 'med'), r = g('dur_recover_tail', 'med');
  if (Number.isFinite(b) && Number.isFinite(rAll) && b > 0) {
    L.push(`③ 전체 ÷ ① = ${(rAll / b).toFixed(2)}배  (밀린 요청 배수 시간이 섞여 있다 — 판정용 아님)`);
  }
  if (Number.isFinite(b) && Number.isFinite(r) && b > 0) {
    const x = r / b;
    L.push(`★ ③ 뒤${TAIL_S}s ÷ ① = ${x.toFixed(2)}배   ← 판정은 이 값으로 한다`);
    // 1.2배는 자의적인 선이다. 노이즈(GC 한 번, 캐시 상태)로 20%는 흔들릴 수 있어서
    // 그 위를 "안 돌아왔다"로 본다. 판단 근거를 숨기지 않으려 여기 적어 둔다.
    L.push(x <= 1.2
      ? '→ 회복함. 부하를 걷으니 평상시 수준으로 돌아왔다.'
      : '→ ★회복 실패. 부하를 원래대로 낮췄는데도 여전히 느리다(메타안정 장애).');
  }
  if (RETRY) L.push(`재시도로 추가 발생한 요청: ${g('retry_sent', 'count')}건 / 총 ${g('req_total', 'count')}건`);
  L.push('');
  L.push('★ RETRY=0과 RETRY=1을 각각 돌려 ③÷① 배수를 비교해야 H5의 답이 나온다.');
  L.push('');

  const out = {};
  out['stdout'] = L.join('\n');
  out[`results/${__ENV.STAMP || 'latest'}-recovery-retry${RETRY ? '1' : '0'}.json`] = JSON.stringify(data, null, 2);
  return out;
}
