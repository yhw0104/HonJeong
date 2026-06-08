// Notices.jsx — 공지사항
// 패턴: 카테고리 칩 필터 + 리스트(중요 핀/NEW 배지·제목·날짜) + 첫 항목 펼침(본문 미리보기)

function Notices() {
  const items = [
    { tag: '업데이트', title: '같이 먹기 신청에 인사 한마디 기능이 추가됐어요', date: '2026.06.02', isNew: true, pinned: true,
      body: '이제 같이 먹기 신청을 보낼 때 짧은 인사를 함께 보낼 수 있어요. 처음 만나는 메이트에게 부담 없이 분위기를 전해보세요.' },
    { tag: '안내', title: '6월 정기 점검 안내 (6/8 새벽 2시~4시)', date: '2026.05.30', isNew: true },
    { tag: '이벤트', title: '첫 혼밥 인증하면 뱃지 2배 지급', date: '2026.05.24', isNew: false },
    { tag: '안내', title: '커뮤니티 이용규칙 개정 안내', date: '2026.05.18', isNew: false },
    { tag: '업데이트', title: '즐겨찾기 그룹 공개 설정 기능 출시', date: '2026.05.10', isNew: false },
  ];

  const tagColor = (t) => t === '업데이트' ? T2.brand : (t === '이벤트' ? '#1B8049' : T2.textSub);

  return (
    <PhoneShell bg={T2.bg}>
      <MoreHeader title="공지사항" />

      {/* 카테고리 칩 */}
      <div style={{ position: 'absolute', top: 108, left: 0, right: 0, zIndex: 9, background: T2.bg, padding: '0 20px 12px' }}>
        <div style={{ display: 'flex', gap: 8 }}>
          {['전체', '업데이트', '이벤트', '안내'].map((c, i) => (
            <div key={i} style={{
              padding: '7px 14px', borderRadius: 999, fontSize: 13, fontWeight: 700, letterSpacing: -0.2, cursor: 'pointer',
              background: i === 0 ? T2.text : '#fff', color: i === 0 ? '#fff' : T2.textSub,
              border: `1px solid ${i === 0 ? T2.text : T2.border}`,
            }}>{c}</div>
          ))}
        </div>
      </div>

      <div style={{ position: 'absolute', top: 160, left: 0, right: 0, bottom: 0, overflow: 'auto', padding: '4px 0 40px' }}>
        <div style={{ background: '#fff', borderTop: `1px solid ${T2.border}`, borderBottom: `1px solid ${T2.border}` }}>
          {items.map((n, i) => (
            <div key={i} style={{
              padding: '16px 20px',
              borderBottom: i < items.length - 1 ? `1px solid ${T2.border}` : 'none',
              cursor: 'pointer',
            }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 7 }}>
                {n.pinned && (
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" style={{ flexShrink: 0 }}>
                    <path d="M9 3h6l-1 6 4 4v2H6v-2l4-4-1-6z" stroke={T2.brand} strokeWidth="1.8" strokeLinejoin="round"/>
                    <path d="M12 15v6" stroke={T2.brand} strokeWidth="1.8" strokeLinecap="round"/>
                  </svg>
                )}
                <span style={{ fontSize: 11, fontWeight: 800, color: tagColor(n.tag), letterSpacing: -0.2 }}>{n.tag}</span>
                {n.isNew && <span style={{ fontSize: 9, fontWeight: 800, color: '#fff', background: T2.brand, padding: '2px 5px', borderRadius: 4, letterSpacing: 0.3 }}>NEW</span>}
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginTop: 7 }}>
                <span style={{ flex: 1, fontSize: 15, fontWeight: 700, color: T2.text, letterSpacing: -0.3, lineHeight: 1.4 }}>{n.title}</span>
                {!n.body && <svg width="16" height="16" viewBox="0 0 24 24" fill="none" style={{ flexShrink: 0 }}><path d="M9 6l6 6-6 6" stroke={T2.textMute} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/></svg>}
                {n.body && <svg width="18" height="18" viewBox="0 0 24 24" fill="none" style={{ flexShrink: 0 }}><path d="M6 15l6-6 6 6" stroke={T2.textMute} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/></svg>}
              </div>
              <div style={{ fontSize: 12, color: T2.textMute, marginTop: 6, fontFeatureSettings: '"tnum"', letterSpacing: -0.2 }}>{n.date}</div>
              {n.body && (
                <div style={{
                  marginTop: 12, padding: 14, background: T2.bg, borderRadius: 12,
                  fontSize: 13, color: T2.textSub, lineHeight: 1.65, letterSpacing: -0.3,
                }}>{n.body}</div>
              )}
            </div>
          ))}
        </div>
      </div>
    </PhoneShell>
  );
}

Object.assign(window, { Notices });
