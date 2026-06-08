// More.jsx — 더보기
// (구 Min_More)

// ───────────────────────────────────────────────────────────
// 화면 5: 더보기 (마이페이지 / 메뉴 리스트)
// ───────────────────────────────────────────────────────────
function More() {
  const sections = [
    {
      title: '나의 혼밥',
      items: [
        { l: '내 혼밥 기록', d: '32회 · 일기 28편', icon: 'book' },
        { l: '혼밥 챌린지 · 뱃지', d: '획득 7 / 20', icon: 'badge', accent: true },
      ],
    },
    {
      title: '메이트',
      items: [
        { l: '받은 같이 먹기 신청', d: '2건', icon: 'mate', badge: '2' },
        { l: '차단 / 신고 관리', d: '', icon: 'shield' },
      ],
    },
    {
      title: '설정',
      items: [
        { l: '알림 설정', d: '', icon: 'bell' },
        { l: '공지사항', d: '', icon: 'note' },
        { l: '고객센터 · 문의', d: '', icon: 'help' },
      ],
    },
  ];

  const Icon = ({ name, c }) => {
    const p = { width: 20, height: 20, viewBox: '0 0 24 24', fill: 'none' };
    const s = { stroke: c, strokeWidth: 1.7, strokeLinecap: 'round', strokeLinejoin: 'round' };
    switch (name) {
      case 'book': return <svg {...p}><path d="M4 5a2 2 0 0 1 2-2h11v16H6a2 2 0 0 0-2 2V5z" {...s}/><path d="M4 19a2 2 0 0 1 2-2h11" {...s}/></svg>;
      case 'badge': return <svg {...p}><circle cx="12" cy="9" r="5.5" {...s}/><path d="M8.5 13.5L7 21l5-2.5L17 21l-1.5-7.5" {...s}/></svg>;
      case 'bookmark': return <svg {...p}><path d="M6 4h12v16l-6-4-6 4V4z" {...s}/></svg>;
      case 'mate': return <svg {...p}><path d="M4 18a5 5 0 0 1 10 0M9 9a3 3 0 1 0 0-.01M17 13a3 3 0 1 0-2-5.2M20 18a4 4 0 0 0-5-3.8" {...s}/></svg>;
      case 'people': return <svg {...p}><circle cx="9" cy="8" r="3" {...s}/><path d="M3.5 19a5.5 5.5 0 0 1 11 0M16 6.5a3 3 0 0 1 0 5M17 19c0-2-1-3.6-2.5-4.5" {...s}/></svg>;
      case 'shield': return <svg {...p}><path d="M12 3l7 3v5c0 4.5-3 7.5-7 9-4-1.5-7-4.5-7-9V6l7-3z" {...s}/></svg>;
      case 'pin': return <svg {...p}><path d="M12 2C8.1 2 5 5.1 5 9c0 5.2 7 13 7 13s7-7.8 7-13c0-3.9-3.1-7-7-7z" {...s}/><circle cx="12" cy="9" r="2.4" {...s}/></svg>;
      case 'bell': return <svg {...p}><path d="M6 9a6 6 0 0 1 12 0c0 5 2 6 2 6H4s2-1 2-6zM10 20a2 2 0 0 0 4 0" {...s}/></svg>;
      case 'note': return <svg {...p}><rect x="5" y="4" width="14" height="16" rx="2" {...s}/><path d="M9 9h6M9 13h6M9 17h3" {...s}/></svg>;
      case 'help': return <svg {...p}><circle cx="12" cy="12" r="9" {...s}/><path d="M9.5 9.5a2.5 2.5 0 0 1 4 1.8c0 1.5-2 2-2 3.2" {...s}/><circle cx="11.5" cy="17" r="0.6" fill={c} stroke="none"/></svg>;
      default: return null;
    }
  };

  return (
    <PhoneShell bg={T2.bg}>
      <div style={{ position: 'absolute', top: 0, left: 0, right: 0, bottom: 78, overflow: 'auto' }}>
        {/* 헤더 */}
        <div style={{ padding: '64px 20px 8px' }}>
          <h1 style={{ fontSize: 28, fontWeight: 800, color: T2.text, letterSpacing: -1, margin: 0 }}>더보기</h1>
        </div>

        {/* 프로필 카드 */}
        <div style={{ padding: '12px 20px 20px' }}>
          <div style={{
            padding: 18, background: '#fff', borderRadius: 18, border: `1px solid ${T2.border}`,
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
              <Avatar name="혼" bg={T2.text} size={52} />
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <span style={{ fontSize: 17, fontWeight: 800, color: T2.text, letterSpacing: -0.4 }}>조용한혼밥러</span>
                </div>
                <div style={{ fontSize: 12, color: T2.textSub, marginTop: 4, letterSpacing: -0.2 }}>혼밥 32회 · 연남동</div>
              </div>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none"><path d="M9 6l6 6-6 6" stroke={T2.textMute} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/></svg>
            </div>

            {/* 팔로워 · 팔로잉 */}
            <div style={{
              display: 'flex', marginTop: 16, paddingTop: 16, borderTop: `1px solid ${T2.border}`,
            }}>
              {[
                { n: '32', l: '혼밥' },
                { n: '7', l: '메이트' },
                { n: '7', l: '뱃지' },
              ].map((s, k) => (
                <div key={k} style={{
                  flex: 1, textAlign: 'center', cursor: 'pointer',
                  borderLeft: k > 0 ? `1px solid ${T2.border}` : 'none',
                }}>
                  <div style={{ fontSize: 18, fontWeight: 800, color: T2.text, letterSpacing: -0.5, fontFeatureSettings: '"tnum"' }}>{s.n}</div>
                  <div style={{ fontSize: 11, color: T2.textMute, marginTop: 3, letterSpacing: -0.2 }}>{s.l}</div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* 섹션들 */}
        {sections.map((sec, si) => (
          <div key={si} style={{ marginTop: si === 0 ? 4 : 18 }}>
            <div style={{ fontSize: 11, fontWeight: 700, color: T2.textMute, letterSpacing: 0.6, textTransform: 'uppercase', padding: '0 20px 8px' }}>
              {sec.title}
            </div>
            <div style={{ background: '#fff', borderTop: `1px solid ${T2.border}`, borderBottom: `1px solid ${T2.border}` }}>
              {sec.items.map((it, ii) => (
                <div key={ii} style={{
                  display: 'flex', alignItems: 'center', gap: 14, padding: '15px 20px',
                  borderBottom: ii < sec.items.length - 1 ? `1px solid ${T2.border}` : 'none',
                  cursor: 'pointer',
                }}>
                  <div style={{
                    width: 36, height: 36, borderRadius: 10, flexShrink: 0,
                    background: it.accent ? T2.brandSoft : T2.bg,
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                  }}>
                    <Icon name={it.icon} c={it.accent ? T2.brand : T2.text} />
                  </div>
                  <span style={{ flex: 1, fontSize: 15, fontWeight: 600, color: T2.text, letterSpacing: -0.3 }}>{it.l}</span>
                  {it.badge && (
                    <span style={{
                      fontSize: 11, fontWeight: 800, color: '#fff', background: T2.brand,
                      minWidth: 18, height: 18, borderRadius: 9, padding: '0 5px',
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                    }}>{it.badge}</span>
                  )}
                  {it.d && <span style={{ fontSize: 13, color: T2.textMute, letterSpacing: -0.2 }}>{it.d}</span>}
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" style={{ flexShrink: 0 }}><path d="M9 6l6 6-6 6" stroke={T2.textMute} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/></svg>
                </div>
              ))}
            </div>
          </div>
        ))}

        {/* 로그아웃 */}
        <div style={{ padding: '24px 20px 32px', display: 'flex', gap: 18 }}>
          <span style={{ fontSize: 13, color: T2.textMute, fontWeight: 600, letterSpacing: -0.2, cursor: 'pointer' }}>로그아웃</span>
          <span style={{ fontSize: 13, color: T2.textMute, fontWeight: 600, letterSpacing: -0.2, cursor: 'pointer' }}>버전 1.0.0</span>
        </div>
      </div>

      <MinTabBar active="more" />
    </PhoneShell>
  );
}

Object.assign(window, { More });
