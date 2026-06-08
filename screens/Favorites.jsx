// Favorites.jsx — 즐겨찾기
// (구 Min_Favorites)

// ───────────────────────────────────────────────────────────
// 화면 4: 즐겨찾기
// ───────────────────────────────────────────────────────────
function Favorites() {
  const [tab, setTab] = React.useState('place');
  const [openGroup, setOpenGroup] = React.useState(null);

  const groups = [
    { id: 'wish', name: '가보고 싶은 곳', note: '혼밥 도전 리스트', emojis: ['🍜', '🍣', '🥘'] },
    { id: 'regular', name: '혼밥 단골', note: '편하게 가는 곳', emojis: ['🍲', '🍱'] },
    { id: 'office', name: '회사 근처 점심', note: '연남 · 합정', emojis: ['🍙', '🍛', '🍔'] },
  ];

  const placesByGroup = {
    wish: [
      { n: '혼밥의자', cat: '일식', dist: '650m', addr: '서대문구 연희로11가길 22 1층', visited: false },
      { n: '연남 파스타바', cat: '양식', dist: '320m', addr: '마포구 동교로 38-12 2층', visited: false },
    ],
    regular: [
      { n: '큰순두부 연남점', cat: '한식', dist: '120m', addr: '마포구 성미산로 161-4', visited: true },
      { n: '옥상국밥', cat: '한식', dist: '480m', addr: '마포구 양화로 64 3층', visited: true },
    ],
    office: [
      { n: '큰순두부 연남점', cat: '한식', dist: '120m', addr: '마포구 성미산로 161-4', visited: true },
    ],
  };

  return (
    <PhoneShell bg={T2.bg}>
      {/* 헤더 */}
      <div style={{ position: 'absolute', top: 60, left: 0, right: 0, padding: '0 20px', zIndex: 10 }}>
        <h1 style={{ fontSize: 28, fontWeight: 800, color: T2.text, letterSpacing: -1, margin: 0 }}>즐겨찾기</h1>

        {/* 세그먼트 */}
        <div style={{ display: 'flex', gap: 20, marginTop: 18 }}>
          <div style={{ position: 'relative', paddingBottom: 12, display: 'flex', alignItems: 'center', gap: 6 }}>
            <span style={{ fontSize: 16, fontWeight: 800, color: T2.text, letterSpacing: -0.3 }}>내 장소</span>
            <span style={{ fontSize: 12, fontWeight: 700, color: T2.brand, fontFeatureSettings: '"tnum"' }}>{groups.length}</span>
            <div style={{ position: 'absolute', left: 0, right: 0, bottom: 0, height: 2, background: T2.brand }} />
          </div>
        </div>
        <div style={{ height: 1, background: T2.border, marginTop: -1 }} />
      </div>

      {/* 리스트 */}
      <div style={{ position: 'absolute', top: 148, left: 0, right: 0, bottom: 92, overflow: 'auto' }}>
        {(
          openGroup === null ? (
            /* 그룹 목록 */
            <div>
              {groups.map((g) => {
                const count = (placesByGroup[g.id] || []).length;
                return (
                  <div key={g.id} onClick={() => setOpenGroup(g.id)} style={{
                    display: 'flex', alignItems: 'center', gap: 14, padding: '14px 20px',
                    borderBottom: `1px solid ${T2.border}`, cursor: 'pointer',
                  }}>
                    {/* 대표 사진 한 장 */}
                    <div style={{
                      width: 52, height: 52, borderRadius: 12, flexShrink: 0,
                      background: T2.mapBg, border: `1px solid ${T2.border}`,
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      fontSize: 22, boxShadow: '0 1px 4px rgba(0,0,0,0.06)',
                    }}>{g.emojis[0]}</div>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ fontSize: 15, fontWeight: 700, color: T2.text, letterSpacing: -0.3 }}>{g.name}</div>
                      <div style={{ fontSize: 12, color: T2.textSub, marginTop: 4, display: 'flex', gap: 7, alignItems: 'center', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                        <span style={{ fontWeight: 700, color: T2.text, fontFeatureSettings: '"tnum"', flexShrink: 0 }}>{count}곳</span>
                        <span style={{ color: T2.textMute, flexShrink: 0 }}>·</span>
                        <span style={{ overflow: 'hidden', textOverflow: 'ellipsis' }}>{g.note}</span>
                      </div>
                    </div>
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" style={{ flexShrink: 0 }}>
                      <path d="M9 6l6 6-6 6" stroke={T2.textMute} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                    </svg>
                  </div>
                );
              })}

              {/* 새 그룹 만들기 */}
              <div style={{
                display: 'flex', alignItems: 'center', gap: 12, padding: '18px 20px',
                cursor: 'pointer',
              }}>
                <div style={{
                  width: 40, height: 40, borderRadius: 10, flexShrink: 0,
                  border: `1.5px dashed ${T2.borderStrong}`,
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  color: T2.textSub, fontSize: 22, fontWeight: 300, lineHeight: 1,
                }}>+</div>
                <span style={{ fontSize: 14, fontWeight: 700, color: T2.textSub, letterSpacing: -0.3 }}>새 그룹 만들기</span>
              </div>
            </div>
          ) : (
            /* 그룹 상세 — 식당 목록 */
            <div>
              <div onClick={() => setOpenGroup(null)} style={{
                display: 'flex', alignItems: 'center', gap: 8, padding: '12px 20px',
                borderBottom: `1px solid ${T2.border}`, cursor: 'pointer',
              }}>
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                  <path d="M15 6l-6 6 6 6" stroke={T2.text} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                </svg>
                <span style={{ fontSize: 15, fontWeight: 800, color: T2.text, letterSpacing: -0.3 }}>
                  {groups.find((g) => g.id === openGroup)?.name}
                </span>
                <span style={{ fontSize: 12, fontWeight: 700, color: T2.brand, marginLeft: 2, fontFeatureSettings: '"tnum"' }}>
                  {(placesByGroup[openGroup] || []).length}
                </span>
              </div>
              {(placesByGroup[openGroup] || []).map((p, i) => (
                <div key={i} style={{
                  display: 'flex', alignItems: 'center', gap: 14, padding: '14px 20px',
                  borderBottom: `1px solid ${T2.border}`,
                }}>
                  <div style={{
                    width: 52, height: 52, borderRadius: 12, flexShrink: 0,
                    background: T2.mapBg, border: `1px solid ${T2.border}`,
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    fontSize: 20, color: T2.textMute,
                  }}>🍽</div>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                      <span style={{ fontSize: 15, fontWeight: 700, color: T2.text, letterSpacing: -0.3, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{p.n}</span>
                      {p.visited && <span style={{
                        fontSize: 10, fontWeight: 700, color: T2.textSub, flexShrink: 0,
                        background: T2.brandSoft, padding: '2px 6px', borderRadius: 5,
                      }}>다녀옴</span>}
                    </div>
                    <div style={{ fontSize: 12, color: T2.textSub, marginTop: 4, display: 'flex', gap: 7, alignItems: 'center', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                      <span style={{ flexShrink: 0 }}>{p.cat}</span>
                      <span style={{ color: T2.textMute, flexShrink: 0 }}>·</span>
                      <span style={{ fontWeight: 700, color: T2.text, flexShrink: 0, fontFeatureSettings: '"tnum"' }}>{p.dist}</span>
                    </div>
                    <div style={{ fontSize: 12, color: T2.textMute, marginTop: 4, letterSpacing: -0.2, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{p.addr}</div>
                  </div>
                </div>
              ))}
            </div>
          )
        )}
      </div>

      {/* 하단 탭 바 */}
      <MinTabBar active="fav" />
    </PhoneShell>
  );
}

Object.assign(window, { Favorites });
