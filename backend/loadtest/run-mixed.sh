#!/usr/bin/env bash
#
# 테스트 6(혼합 부하) 실행기. "실제로 앱을 쓰는 사용자 N명"을 흉내내 동시 사용자 한계를 찾는다.
#
#   ./run-mixed.sh                                  # 300→600→1000→1500→300명 × 60초
#   USERS=300,600 DURATION_S=90 ./run-mixed.sh
#   P_SEARCH=0.5 ./run-mixed.sh                     # 검색을 더 많이 하는 가정으로
#
# ★GC 오버레이는 필요 없다(테스트 3과 달리 힙 가설을 보는 게 아니다).
#   docker compose -f docker-compose.yml -f docker-compose.loadtest.yml up -d db app
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$HERE"

# pg_stat_activity 조회에 DB_PASSWORD가 필요하다.
set -a; . ../.env; set +a

STAMP="$(date +%Y%m%d-%H%M%S)"
mkdir -p results

# ── 사전 점검 ────────────────────────────────────────────────────────────────
# 실패하면 몇 분 뒤에 빈 결과를 보는 대신 지금 멈춘다.
[ -f tokens.json ] || { echo "tokens.json이 없다. ./seed-users.sh 를 먼저 돌릴 것." >&2; exit 1; }
ntok="$(jq 'length' tokens.json)"
# ★폴링 테스트는 동시 접속자를 흉내내는 것이라 사용자가 실제로 여러 명이어야 한다. 토큰이
#   모자라면 소수의 사용자가 돌려쓰게 되는데, 그러면 /conversations·/meal-requests 응답이
#   비현실적으로 캐시 친화적이 되어 부하가 과소평가된다.
# ★혼합 테스트는 수천 명을 흉내내므로 토큰을 돌려쓴다(사용자당 토큰 1:1을 요구하면
#   1,500명을 재려고 1,500명을 시드해야 한다). 다만 너무 적으면 /conversations 응답이
#   비현실적으로 캐시 친화적이 되어 부하가 과소평가되므로 300개는 요구한다.
need=300
[ "$ntok" -ge "$need" ] || { echo "토큰이 ${ntok}개뿐이다. ./seed-users.sh ${need} 를 먼저 돌릴 것." >&2; exit 1; }

code="$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/api/health || true)"
[ "$code" = "200" ] || { echo "앱이 응답하지 않는다(health=$code). compose를 먼저 띄울 것." >&2; exit 1; }

# ★토큰이 아직 유효한지 확인한다. access token TTL이 1시간(application.yml)이라, 테스트를
#   여러 개 이어 돌리다 보면 도중에 만료된다. 만료되면 전 요청이 401을 4ms에 받는데 —
#   실패율을 안 보면 "서버가 엄청 빨라졌다"로 읽힌다. 실제로 08-24에 그렇게 한 번 속았다.
probe="$(curl -s -o /dev/null -w '%{http_code}' -H "Authorization: Bearer $(jq -r '.[0]' tokens.json)" \
         http://localhost:8080/api/check-ins/me || true)"
[ "$probe" = "200" ] || { echo "토큰이 만료됐다(HTTP $probe). ./seed-users.sh $ntok 로 다시 발급할 것." >&2; exit 1; }


# ★제한이 걸린 컨테이너인지 확인한다. 제한 없이 재면 맥 전체 성능으로 측정돼 운영과 무관한
#   숫자가 나오는데, 결과만 보면 그 사실이 드러나지 않는다(그래서 여기서 막는다).
cpus="$(docker inspect honjeong-app --format '{{.HostConfig.NanoCpus}}')"
mem="$(docker inspect honjeong-app --format '{{.HostConfig.Memory}}')"
# ★cpuset은 app·db 둘 다 확인한다. db가 안 묶여 있으면 postgres 병렬 스캔이 맥의 남은 코어를
#   전부 끌어써서, 재는 게 운영 한계가 아니라 이 맥의 한계가 된다(포화점 테스트에선 치명적).
for c in honjeong-app honjeong-db; do
  cs="$(docker inspect "$c" --format '{{.HostConfig.CpusetCpus}}')"
  [ "$cs" = "0,1" ] || { echo "★ $c 가 코어에 묶여 있지 않다 (cpuset='$cs'). 운영 2vCPU 흉내가 안 된다." >&2; exit 1; }
done
[ "$cpus" = "2000000000" ] && [ "$mem" = "1992294400" ] || {
  echo "★ honjeong-app에 운영 사양 제한이 없다 (cpus=$cpus mem=$mem)." >&2
  echo "  docker-compose.loadtest.yml을 겹쳐서 다시 띄울 것:" >&2
  echo "  docker compose -f docker-compose.yml -f docker-compose.loadtest.yml up -d db app" >&2
  exit 1
}

echo "토큰 ${ntok}개 · 제한 2CPU/1900MB · 결과 접두사 $STAMP"

# ── 자원 수집기 ──────────────────────────────────────────────────────────────
# ★docker stats 대신 cgroup을 직접 읽는다. `docker stats --no-stream`은 호출 한 번에 1~2초가
#   걸려 40초 테스트에서 표본이 20개밖에 안 잡혔고, 값도 짧은 내부 구간의 근사치라 스파이크를
#   놓친다. cgroup v2의 cpu.stat(usage_usec)은 단조 증가 누적값이라, 델타를 실제 경과시간으로
#   나누면 그 구간의 CPU 사용률이 정확히 나온다.
statsfile="results/$STAMP-mixed-stats.csv"
echo "time,app_cpu_pct,app_mem_mb,db_cpu_pct,db_mem_mb,pg_active,pg_total" > "$statsfile"
(
  # ★타임스탬프는 perl로 받는다. macOS의 date는 %N(나노초)을 지원하지 않아 `date +%s%6N`이
  #   "...276N" 같은 쓰레기를 조용히 뱉는다(exit 0이라 `||` 대체도 안 걸린다).
  now_us() { perl -MTime::HiRes=time -e 'printf "%d", time()*1000000'; }
  # ★app만이 아니라 db도 잰다. 처음엔 app만 쟀는데 CPU가 5.6%로 나와 "서버가 논다"고 읽혔다.
  #   실제로는 app이 DB 응답을 기다리는 중이었고 일은 전부 postgres가 하고 있었다 — 측정 대상을
  #   하나 빠뜨리면 병목이 없는 것처럼 보인다.
  read_one() { docker exec "$1" sh -c \
      'awk "/^usage_usec/{printf \"%s \", \$2}" /sys/fs/cgroup/cpu.stat; cat /sys/fs/cgroup/memory.current' 2>/dev/null; }
  pct() { awk -v a="$1" -v b="$2" -v x="$3" -v y="$4" 'BEGIN{d=x-y; if(d>0) printf "%.1f", (a-b)*100/d}'; }

  # ★actuator가 없어 Hikari 풀 상태를 앱에서 못 본다. 대신 DB 쪽에서 센다 —
  #   앱이 커넥션 10개를 다 쓰고 있으면 여기 active가 10 근처에 붙는다. 그게 풀 포화의 증거다.
  pg_probe() { docker exec -e PGPASSWORD="$DB_PASSWORD" honjeong-db psql -U honjeong -d honjeong -tAc \
      "SELECT count(*) FILTER (WHERE state='active')||','||count(*) FROM pg_stat_activity WHERE datname='honjeong' AND backend_type='client backend'" 2>/dev/null; }
  pa=""; pd=""; prev_t=""
  while :; do
    read -r au am <<<"$(read_one honjeong-app)"
    read -r du dm <<<"$(read_one honjeong-db)"
    t="$(now_us)"
    if [ -n "${au:-}" ] && [ -n "$pa" ]; then
      # 100% = 코어 1개. app은 2코어로 제한돼 상한 200%, db는 제한이 없다(아래 주석 참고).
      echo "$(date +%H:%M:%S),$(pct "$au" "$pa" "$t" "$prev_t"),$(( ${am:-0} / 1048576 )),$(pct "$du" "$pd" "$t" "$prev_t"),$(( ${dm:-0} / 1048576 )),$(pg_probe)" >> "$statsfile"
    fi
    pa="${au:-}"; pd="${du:-}"; prev_t="$t"
    sleep 1
  done
) &
sampler=$!
# kill 시 셸이 "Terminated" 잡메시지를 찍지 않게 작업 테이블에서 뗀다.
trap 'kill "$sampler" 2>/dev/null || true' EXIT
disown "$sampler" 2>/dev/null || true

# ── 실행 ────────────────────────────────────────────────────────────────────
T0="$(date +%s)"   # 단계별 CPU 구간을 역산하는 기준점
STAMP="$STAMP" k6 run \
  --env "STAMP=$STAMP" \
  --env "USERS=${USERS:-300,600,1000,1500,300}" \
  --env "DURATION_S=${DURATION_S:-60}" \
  --env "P_SEARCH=${P_SEARCH:-0.30}" \
  --env "P_DETAIL=${P_DETAIL:-0.20}" \
  --env "P_CHAT=${P_CHAT:-0.15}" \
  mixed.js

kill "$sampler" 2>/dev/null || true

# ── 단계별 자원 ──────────────────────────────────────────────────────────────
# ★단계마다 따로 본다. 전체 평균만 내면 저부하 단계가 고부하 단계를 희석해서
#   "어디서 포화하는가"라는 이 테스트의 질문에 답할 수 없다. k6의 startTime 계산식이
#   결정적이라 시작 시각(T0)만 알면 각 단계의 벽시계 구간을 역산할 수 있다.
echo
echo "단계별 자원 (CPU 100% = 코어 1개, app+db가 2코어를 나눠 쓰므로 200%가 상한)"
python3 - "$statsfile" "$T0" "${USERS:-300,600,1000,1500,300}" \
         "${DURATION_S:-60}" "${GAP_S:-20}" "${WARMUP_S:-60}" <<'PYSTEP'
import sys, csv, datetime
path, t0, users, dur, gap, warm = sys.argv[1:7]
t0, dur, gap, warm = int(t0), int(dur), int(gap), int(warm)
steps = [int(x) for x in users.split(',')]

rows = []
base = datetime.datetime.fromtimestamp(t0)
with open(path) as f:
    for r in csv.DictReader(f):
        if not r["app_cpu_pct"]:
            continue
        hh, mm, ss = map(int, r["time"].split(":"))
        t = base.replace(hour=hh, minute=mm, second=ss, microsecond=0)
        if t.timestamp() < t0 - 3600:          # 자정을 넘긴 경우
            t += datetime.timedelta(days=1)
        rows.append((t.timestamp(), float(r["app_cpu_pct"]),
                     float(r["db_cpu_pct"]), int(r["pg_active"] or 0)))

print("  사용자    app CPU     db CPU    합계CPU    커넥션")
for i, n in enumerate(steps):
    s0 = t0 + warm + gap + i * (dur + gap)
    # 앞 3초는 램프업이라 버린다 — 넣으면 구간 평균이 낮게 나온다
    w = [x for x in rows if s0 + 3 <= x[0] <= s0 + dur]
    if not w:
        print(f"{n:>8}         (표본 없음)")
        continue
    a = sum(x[1] for x in w) / len(w)
    d = sum(x[2] for x in w) / len(w)
    c = max(x[3] for x in w)
    print(f"{n:>8}{a:>10.1f}%{d:>10.1f}%{a+d:>10.1f}%{c:>8}/10"
          + ("   ← 포화" if a + d >= 180 else ""))
PYSTEP

echo
echo "★ app·db를 코어 0,1에 함께 묶어 운영 2vCPU를 흉내냈다. 다만 맥 코어가 Lightsail보다"
echo "  3~4배 빠르므로, 여기서 나온 초당 건수는 운영에서 그만큼 낮아진다고 봐야 한다."
