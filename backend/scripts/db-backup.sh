#!/usr/bin/env bash
#
# 혼정 운영 DB 일일 백업. 서버(Lightsail)의 cron이 부른다.
#
# ★Lightsail 자동 스냅샷과 역할이 다르다. 스냅샷은 "디스크·인스턴스가 통째로 날아간 경우"를
#   막고, 이 스크립트는 그보다 훨씬 자주 나는 사고 — 서버에서 `docker compose down -v`를 치거나
#   (런북이 굵게 경고하는 그것) 마이그레이션이 데이터를 망가뜨리는 경우 — 를 막는다.
#   둘 다 있어야 한다. 이 파일은 같은 디스크에 남으므로 디스크 손실은 못 막는다.
#
# 설치 위치는 저장소 밖이다(/home/ubuntu/bin/). 배포 tar가 `scripts`를 제외하고
# `rm -rf ~/project/backend/src`도 하므로, 저장소 안에 두면 배포 때마다 사라지거나 낡는다.
# 여기(git)에 정본을 두고 서버로는 scp로 올린다 — 고칠 때도 scp가 필요하다.
set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-/home/ubuntu/backups}"
KEEP_DAYS="${KEEP_DAYS:-14}"
# 정상 덤프는 gzip 후 수십 MB다(places가 313MB를 차지한다). 이보다 작으면 뭔가 잘못된 것이다.
MIN_BYTES="${MIN_BYTES:-1000000}"

mkdir -p "$BACKUP_DIR"

stamp="$(date -u +%Y%m%d-%H%M%SZ)"   # 호스트가 UTC다. 파일명에 Z를 붙여 오해를 막는다.
final="$BACKUP_DIR/honjeong-$stamp.sql.gz"
tmp="$final.partial"

# 중간에 죽으면 반쪽짜리 파일이 남는데, 그게 최종 이름을 갖고 있으면 "백업이 있다"고 착각하게 된다.
# .partial로 받고 검증을 통과한 것만 이름을 바꾼다.
trap 'rm -f "$tmp"' EXIT

# ★set -o pipefail이 없으면 pg_dump가 실패해도 gzip이 성공해 스크립트가 0으로 끝난다.
#   그러면 "매일 도는데 내용이 빈" 백업이 쌓이고, 그 사실은 복구하려는 순간에야 드러난다.
#   (set -euo pipefail에 포함돼 있다 — 지우지 말 것.)
docker exec honjeong-db pg_dump -U honjeong -d honjeong | gzip -9 > "$tmp"

gzip -t "$tmp"                       # 압축이 온전한가
size="$(stat -c %s "$tmp")"
if [ "$size" -lt "$MIN_BYTES" ]; then
    echo "$(date -u '+%F %T')Z FAIL 백업이 비정상적으로 작다(${size}B < ${MIN_BYTES}B) — 버린다" >&2
    exit 1
fi

mv "$tmp" "$final"
trap - EXIT

# 백업이 언제까지 정상이었는지 한 줄로 확인할 수 있게 남긴다.
# 점검할 때: cat /home/ubuntu/backups/LAST_SUCCESS
date -u '+%F %T Z' > "$BACKUP_DIR/LAST_SUCCESS"
echo "$(date -u '+%F %T')Z OK $final ($((size / 1024 / 1024))MB)"

# 오래된 것 정리. .partial은 하루만 지나도 지운다(실패한 잔해다).
find "$BACKUP_DIR" -maxdepth 1 -name 'honjeong-*.sql.gz' -type f -mtime "+$KEEP_DAYS" -delete
find "$BACKUP_DIR" -maxdepth 1 -name 'honjeong-*.sql.gz.partial' -type f -mtime +1 -delete
