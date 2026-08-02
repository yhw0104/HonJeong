#!/usr/bin/env bash
# places 테이블만 덤프한다 — 배포 서버로 식당 데이터를 옮기는 용도.
#
# 계정·체크인·리뷰 등 나머지 테이블은 옮기지 않는다(서버는 새로 시작한다).
# 스키마는 서버에서 Flyway가 만들므로 데이터만 넣는다(--data-only).
#
# 사용법:  backend/scripts/dump-places.sh [출력경로]
# 기본 출력: backend/scripts/places.sql.gz
set -euo pipefail

CONTAINER="${DB_CONTAINER:-honjeong-db}"
OUT="${1:-$(cd "$(dirname "$0")" && pwd)/places.sql.gz}"

if ! docker ps --format '{{.Names}}' | grep -qx "$CONTAINER"; then
  echo "오류: 컨테이너 '$CONTAINER'가 실행 중이 아닙니다. 먼저 docker compose up -d db 를 실행하세요." >&2
  exit 1
fi

echo "덤프 대상 행 수 확인 중..."
ROWS=$(docker exec "$CONTAINER" psql -U honjeong -d honjeong -tAc "SELECT count(*) FROM places;")
echo "places: ${ROWS}행"

echo "덤프 중 → ${OUT}"
docker exec "$CONTAINER" pg_dump -U honjeong -d honjeong \
  --data-only --table=places --no-owner --no-privileges \
  | gzip > "$OUT"

echo "완료: $(ls -lh "$OUT" | awk '{print $5}')"
echo
echo "서버 복원 절차는 docs/작업정리/2026-08-02-배포-런북.md 를 참고하세요."
