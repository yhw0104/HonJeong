#!/usr/bin/env bash
#
# 테스트 4(회복) 실행기. RETRY=0과 RETRY=1을 연달아 돌려 비교한다.
#
#   ./run-recovery.sh              # 재시도 없음 → 재시도 있음, 2회 연속(약 11분)
#   ONLY=1 ./run-recovery.sh       # 재시도 있는 쪽만
#
# ★두 번 돌려야 의미가 있다. "복귀 구간이 느리다"만으로는 서버가 원래 느린 건지 재시도가
#   물고 늘어지는 건지 알 수 없다. 재시도 유무의 차이가 곧 가설 H5의 답이다.
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$HERE"
set -a; . ../.env; set +a

STAMP="$(date +%Y%m%d-%H%M%S)"
mkdir -p results

# ── 사전 점검 ────────────────────────────────────────────────────────────────
# 실패하면 몇 분 뒤에 빈 결과를 보는 대신 지금 멈춘다.
[ -f tokens.json ] || { echo "tokens.json이 없다. ./seed-users.sh 를 먼저 돌릴 것." >&2; exit 1; }
ntok="$(jq 'length' tokens.json)"
[ "$ntok" -ge 1 ] || { echo "tokens.json이 비었다." >&2; exit 1; }

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

echo "토큰 ${ntok}개 · 제한 2CPU/1900MB 확인 · 결과 접두사 $STAMP"

# ── 자원 수집기 ──────────────────────────────────────────────────────────────
# ★docker stats 대신 cgroup을 직접 읽는다. `docker stats --no-stream`은 호출 한 번에 1~2초가
#   걸려 40초 테스트에서 표본이 20개밖에 안 잡혔고, 값도 짧은 내부 구간의 근사치라 스파이크를
#   놓친다. cgroup v2의 cpu.stat(usage_usec)은 단조 증가 누적값이라, 델타를 실제 경과시간으로
#   나누면 그 구간의 CPU 사용률이 정확히 나온다.
statsfile="results/$STAMP-recovery-stats.csv"
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
# ONLY=1이면 재시도 있는 쪽만, 아니면 없음 → 있음 순서로 두 번.
if [ -n "${ONLY:-}" ]; then modes="1"; else modes="0 1"; fi

for RETRY in $modes; do
  echo
  if [ "$RETRY" = 1 ]; then echo "══════ 재시도 ON (앱의 retry:1 재현) ══════"
  else echo "══════ 재시도 OFF ══════"; fi

  STAMP="$STAMP" k6 run \
    --env "STAMP=$STAMP" \
    --env "RETRY=$RETRY" \
    --env "BASE_RATE=${BASE_RATE:-2}" \
    --env "PEAK_RATE=${PEAK_RATE:-8}" \
    --env "PEAK_S=${PEAK_S:-60}" \
    --env "RECOVER_S=${RECOVER_S:-180}" \
    --env "TAIL_S=${TAIL_S:-60}" \
    recovery.js

  # ★두 번째 실행 전에 반드시 쉰다. 앞 실행의 대기열·GC·계획 캐시가 남은 채로 시작하면
  #   두 번째가 불리해져서, 재시도 탓인지 앞선 부하의 잔재인지 구분할 수 없게 된다.
  if [ "$RETRY" = 0 ] && [ "$modes" = "0 1" ]; then echo "…60초 진정 대기"; sleep 60; fi
done

kill "$sampler" 2>/dev/null || true

# ── CPU 요약 ────────────────────────────────────────────────────────────────
# 변형별로 쪼개지 않고 전체 최대/평균만 본다. 저부하 비교 테스트라 CPU는 "포화하지 않았음"을
# 확인하는 용도다 — 포화했다면 응답시간 비교 자체가 경합에 오염된 것이라 다시 재야 한다.
echo
echo "컨테이너 자원 (CPU 100% = 코어 1개)"
awk -F, 'NR>1 && $2 != "" { a+=$2; d+=$4; n++; if($2>ax)ax=$2; if($4>dx)dx=$4; if($3>am)am=$3; if($5>dm)dm=$5 }
         NR>1 && $6 != "" { if($6+0>ca+0) ca=$6 }   # ★NR>1 없으면 헤더의 "pg_active"가 ca에 들어가 이후 숫자 비교가 전부 거짓이 된다
         END { if(n){ printf "  app  CPU 평균 %.1f%% 최대 %.1f%%  (상한 200%%)  메모리 최대 %d MB\n", a/n, ax, am
                      printf "  db   CPU 평균 %.1f%% 최대 %.1f%%  (제한 없음)   메모리 최대 %d MB\n", d/n, dx, dm
                      printf "  합계 CPU 평균 %.1f%%  (2코어 = 200%%가 상한)\n", (a+d)/n
                      printf "  DB 커넥션 active 최대 %d개  (Hikari 풀 = 10)\n", ca
               } else print "  표본 없음" }' "$statsfile"
echo "  원본: $statsfile"
echo
echo "★ app·db를 코어 0,1에 함께 묶어 운영 2vCPU를 흉내냈다. 다만 맥 코어가 Lightsail보다"
echo "  3~4배 빠르므로, 여기서 나온 초당 건수는 운영에서 그만큼 낮아진다고 봐야 한다."
