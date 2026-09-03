#!/usr/bin/env python3
"""
채팅 목록 N+1 측정용 — 가상 사용자에게 대화방을 만들어 준다.

ConversationService.listMine()은 대화방마다 countUnread를 한 번씩 부른다(N+1).
그런데 08-24 부하 테스트에서는 가상 사용자에게 대화가 0건이라 N+1이 발생조차 하지 않았다.
"문제가 있는지 없는지 모르는" 상태였고, 이 스크립트가 그 전제를 만든다.

측정 설계 — 사용자를 대화방 수로 나눈다. 같은 사용자에게 대화를 늘려가며 재면
캐시·계획 상태가 섞이므로, 처음부터 서로 다른 집단으로 갈라 둔다.

    그룹 0    50명 × 0개    ← 대조군. N+1이 아예 안 도는 경우
    그룹 5    50명 × 5개
    그룹 20   50명 × 20개
    그룹 50   50명 × 50개
    파트너   100명          ← 상대역. 이들도 대화가 쌓이지만 측정하지 않는다

★파트너를 따로 두는 이유: 대화는 양쪽 목록에 다 뜬다. 그룹 0을 상대역으로 쓰면
  그룹 0이 더 이상 0개가 아니게 되어 대조군이 무너진다.

★읽음 시각(last_read_at)을 NULL로 둔다 = 한 번도 안 읽음. countUnread가 상대 메시지를
  전부 세게 되어 N+1의 비용이 가장 크게 드러난다.

사용법:  ./seed-users.sh 300  후  python3 seed-conversations.py
출력:    conv-groups.json (k6가 그룹별 토큰을 읽는다)
"""
import base64, json, os, pathlib, subprocess, sys

HERE = pathlib.Path(__file__).parent
GROUPS = [0, 5, 20, 50]          # 그룹별 대화방 수
PER_GROUP = 50                   # 그룹당 사용자 수
MSGS_PER_CONV = 10               # 대화방당 메시지 수(양쪽 반씩)

def sub_of(token: str) -> int:
    """JWT payload의 sub(사용자 id)를 꺼낸다. base64url은 패딩을 채워야 디코드된다."""
    p = token.split('.')[1]
    p += '=' * (-len(p) % 4)
    return int(json.loads(base64.urlsafe_b64decode(p))['sub'])

def psql(sql: str, pw: str, quiet=True) -> str:
    cmd = ['docker', 'exec', '-i', '-e', f'PGPASSWORD={pw}', 'honjeong-db',
           'psql', '-U', 'honjeong', '-d', 'honjeong', '-v', 'ON_ERROR_STOP=1']
    if quiet:
        cmd += ['-tAq']
    r = subprocess.run(cmd, input=sql, capture_output=True, text=True)
    if r.returncode != 0:
        sys.exit(f"psql 실패:\n{r.stderr}")
    return r.stdout.strip()

def main():
    pw = os.environ.get('DB_PASSWORD')
    if not pw:
        sys.exit("DB_PASSWORD가 없다. `set -a; . ../.env; set +a` 후 실행할 것.")

    tokens = json.loads((HERE / 'tokens.json').read_text())
    need = PER_GROUP * len(GROUPS) + 100
    if len(tokens) < need:
        sys.exit(f"토큰이 {len(tokens)}개뿐이다. {need}개가 필요하다 — ./seed-users.sh {need}")

    # 사용자 id 순으로 정렬해 그룹을 고정한다(토큰 파일 순서는 병렬 생성 순서라 매번 달라진다).
    users = sorted(((sub_of(t), t) for t in tokens), key=lambda x: x[0])
    groups, i = {}, 0
    for n in GROUPS:
        groups[n] = users[i:i + PER_GROUP]
        i += PER_GROUP
    partners = [uid for uid, _ in users[i:i + 100]]

    place_id = psql("SELECT id FROM places WHERE business_status='영업' LIMIT 1", pw)

    # 이 스크립트가 만든 것만 지운다. 사람이 쓴 대화는 건드리지 않는다.
    # loadtest 사용자가 낀 대화 → 메시지 → 대화 → 신청 → 체크인 순(자식부터).
    print("기존 부하테스트 대화 정리 중...")
    psql(f"""
BEGIN;
CREATE TEMP TABLE lt ON COMMIT DROP AS
  SELECT u.id FROM users u JOIN social_accounts sa ON sa.user_id=u.id
  WHERE sa.provider_user_id LIKE 'mock-kakao-loadtest-%';
DELETE FROM chat_messages WHERE conversation_id IN (
  SELECT id FROM conversations WHERE from_user_id IN (SELECT id FROM lt) OR to_user_id IN (SELECT id FROM lt));
DELETE FROM conversations WHERE from_user_id IN (SELECT id FROM lt) OR to_user_id IN (SELECT id FROM lt);
DELETE FROM meal_requests WHERE from_user_id IN (SELECT id FROM lt)
   OR to_check_in_id IN (SELECT id FROM check_ins WHERE user_id IN (SELECT id FROM lt));
DELETE FROM check_ins WHERE user_id IN (SELECT id FROM lt);
COMMIT;""", pw)

    # ★체크인 status를 ENDED로 만든다. uq_check_ins_current_user가
    #   SEEKING/ACTIVE/TOGETHER에만 걸리는 부분 유니크 인덱스라, ENDED면 한 사용자가
    #   여러 개를 가질 수 있다(그룹 50은 사용자당 50개가 필요하다).
    rows = []
    for n, members in groups.items():
        for slot, (uid, _) in enumerate(members):
            for k in range(n):
                partner = partners[(slot * 7 + k) % len(partners)]
                # 절반은 내가 신청자, 절반은 내가 수신자 — 목록 쿼리가 양쪽을 다 훑는다.
                a, b = (uid, partner) if k % 2 == 0 else (partner, uid)
                rows.append((a, b))

    print(f"대화방 {len(rows)}개 생성 중 (메시지 {len(rows) * MSGS_PER_CONV}개)...")
    values = ",".join(f"({a},{b})" for a, b in rows)
    psql(f"""
BEGIN;
CREATE TEMP TABLE pairs(from_id bigint, to_id bigint) ON COMMIT DROP;
INSERT INTO pairs VALUES {values};
ALTER TABLE pairs ADD COLUMN ci_id bigint, ADD COLUMN mr_id bigint, ADD COLUMN cv_id bigint;

-- 체크인(수신자 소유) → 신청 → 대화 순으로 사슬을 만든다.
WITH ins AS (
  INSERT INTO check_ins (user_id, place_id, status, started_at, ended_at, created_at)
  SELECT to_id, {place_id}, 'ENDED', now(), now(), now() FROM pairs RETURNING id, user_id)
UPDATE pairs p SET ci_id = s.id FROM (
  SELECT id, user_id, row_number() OVER (PARTITION BY user_id ORDER BY id) rn FROM ins) s
WHERE s.user_id = p.to_id AND s.rn = (
  SELECT count(*) FROM pairs q WHERE q.to_id = p.to_id AND q.ctid <= p.ctid);

INSERT INTO meal_requests (from_user_id, to_check_in_id, place_id, status, created_at, responded_at)
SELECT from_id, ci_id, {place_id}, 'ACCEPTED', now(), now() FROM pairs;

UPDATE pairs p SET mr_id = m.id FROM (
  SELECT id, to_check_in_id FROM meal_requests) m WHERE m.to_check_in_id = p.ci_id;

INSERT INTO conversations (meal_request_id, place_id, from_user_id, to_user_id, status,
                           last_message_at, created_at, updated_at, from_muted, to_muted)
SELECT mr_id, {place_id}, from_id, to_id, 'ACTIVE', now(), now(), now(), false, false FROM pairs;

UPDATE pairs p SET cv_id = c.id FROM conversations c WHERE c.meal_request_id = p.mr_id;

-- 메시지: 양쪽이 번갈아 보낸다. 읽음 시각은 NULL로 두어 전부 안읽음이 되게 한다.
INSERT INTO chat_messages (conversation_id, sender_user_id, type, text, created_at)
SELECT p.cv_id,
       CASE WHEN g % 2 = 0 THEN p.from_id ELSE p.to_id END,
       'TEXT', '부하테스트 메시지 ' || g, now()
FROM pairs p, generate_series(1, {MSGS_PER_CONV}) g;
COMMIT;""", pw)

    out = {str(n): [t for _, t in members] for n, members in groups.items()}
    (HERE / 'conv-groups.json').write_text(json.dumps(out, ensure_ascii=False))

    print("\n생성 결과 — 그룹별 실제 대화방 수(사용자 1명 기준):")
    for n, members in groups.items():
        uid = members[0][0]
        got = psql(f"SELECT count(*) FROM conversations WHERE from_user_id={uid} OR to_user_id={uid}", pw)
        mark = "OK" if int(got) == n else "★불일치"
        print(f"  그룹 {n:>2}개 → 실제 {got:>2}개  {mark}")
    print(f"\n출력: {HERE / 'conv-groups.json'}")

if __name__ == '__main__':
    main()
