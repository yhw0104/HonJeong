#!/usr/bin/env bash
#
# 부하 테스트용 가상 사용자 N명을 만들고 accessToken을 tokens.json으로 떨군다.
#
#   ./seed-users.sh [명수] [베이스URL]      # 기본 300명, http://localhost:8080
#
# ★로컬 전용이다. 운영에서는 애초에 돌지 않는다 — OAUTH_MODE=real이면 MockOAuthVerifier가
#   빈에 등록되지 않아 아무 문자열이나 idToken으로 통과시키는 경로 자체가 없다. 그리고 배포
#   프로파일은 OAuthRealModeCheck가 mock 기동을 막는다. 즉 이 스크립트는 구조적으로 로컬에서만 된다.
#
# ★멱등하다. MockOAuthVerifier가 providerUserId를 "mock-kakao-<idToken>"으로 합성하므로,
#   같은 idToken은 언제나 같은 사용자다. 다시 돌리면 새로 만들지 않고 로그인만 해서 토큰을 갱신한다.
#
# 정리는 cleanup-users.sql 참고.
set -euo pipefail

COUNT="${1:-300}"
BASE="${2:-http://localhost:8080}"
PARALLEL="${PARALLEL:-8}"

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUT="$HERE/tokens.json"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

# 사용자 한 명을 확보하고 accessToken 한 줄을 출력한다.
# 신규면 oauth→terms→complete 3단계, 기존이면 oauth 한 번으로 끝난다.
mint_one() {
  local i="$1" base="$2" tmp="$3"
  local idtoken="loadtest-$i"

  local res onboarding access
  res="$(curl -sS -X POST "$base/api/auth/oauth/kakao" \
          -H 'Content-Type: application/json' \
          -d "{\"idToken\":\"$idtoken\"}")" || { echo "[$i] oauth 요청 실패" >&2; return 1; }

  if [ "$(jq -r '.success' <<<"$res")" != "true" ]; then
    echo "[$i] oauth 거절: $(jq -c '.error' <<<"$res")" >&2; return 1
  fi

  access="$(jq -r '.data.accessToken // empty' <<<"$res")"
  if [ -n "$access" ]; then                      # 이미 온보딩을 마친 기존 사용자
    printf '%s\n' "$access" >> "$tmp/tokens"
    return 0
  fi

  onboarding="$(jq -r '.data.onboardingToken // empty' <<<"$res")"
  if [ -z "$onboarding" ]; then
    echo "[$i] 토큰이 응답에 없다: $res" >&2; return 1
  fi

  # 약관 동의. marketing만 false로 둬 실제 가입 흐름과 같은 모양을 만든다.
  curl -sS -o /dev/null -X POST "$base/api/auth/terms" \
    -H "Authorization: Bearer $onboarding" -H 'Content-Type: application/json' \
    -d '{"age":true,"service":true,"privacy":true,"location":true,"marketing":false}' \
    || { echo "[$i] terms 실패" >&2; return 1; }

  # nickname은 users 테이블에서 UNIQUE다(V1__core.sql:16). 인덱스로 구분한다.
  res="$(curl -sS -X POST "$base/api/auth/complete" \
          -H "Authorization: Bearer $onboarding" -H 'Content-Type: application/json' \
          -d "{\"nickname\":\"부하$i\"}")" || { echo "[$i] complete 요청 실패" >&2; return 1; }

  access="$(jq -r '.data.accessToken // empty' <<<"$res")"
  if [ -z "$access" ]; then
    echo "[$i] complete 거절: $(jq -c '.error // .' <<<"$res")" >&2; return 1
  fi
  printf '%s\n' "$access" >> "$tmp/tokens"
}
export -f mint_one

echo "가상 사용자 ${COUNT}명 확보 중 ($BASE, 동시 $PARALLEL) ..."
: > "$TMP/tokens"
# 실패한 사용자가 있어도 나머지는 계속 만든다(|| true). 개수는 아래서 확인한다.
seq 1 "$COUNT" | xargs -P "$PARALLEL" -I{} bash -c 'mint_one "$@"' _ {} "$BASE" "$TMP" || true

got="$(wc -l < "$TMP/tokens" | tr -d ' ')"
jq -R -s 'split("\n") | map(select(length > 0))' < "$TMP/tokens" > "$OUT"

echo "확보: $got / $COUNT"
echo "출력: $OUT"
[ "$got" -eq "$COUNT" ] || { echo "★ 일부 실패했다. 위 stderr를 확인할 것." >&2; exit 1; }
