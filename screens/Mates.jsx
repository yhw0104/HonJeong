// Mates.jsx — 메이트
// (구 Min_MateList)

// ───────────────────────────────────────────────────────────
// 더보기 6: 메이트 (내 메이트 + 친구 추가)
// ───────────────────────────────────────────────────────────
function Mates() {
  const myMates = [
    { name: '점심혼밥러', emo: '🍙', meta: '연남동 · 혼밥 32회', tags: ['한식', '대화 OK'], together: 2, now: true, nowPlace: '큰순두부 연남점' },
    { name: '조용한미식가', emo: '🍜', meta: '합정 · 혼밥 18회', tags: ['일식', '조용히'], together: 1, now: false },
    { name: '연남책방지기', emo: '📚', meta: '연남동 · 혼밥 12회', tags: ['면 요리', '대화 OK'], together: 0, now: true, nowPlace: '혼밥의자' },
  ];
  const suggest = [
    { name: '국밥러버', emo: '🍲', meta: '망원 · 혼밥 41회', mutual: 3, state: 'add' },
    { name: '디저트헌터', emo: '🍰', meta: '상수 · 혼밥 9회', mutual: 1, state: 'sent' },
  ];

  const labelStyle = { fontSize: 11, fontWeight: 700, color: T2.textMute, letterSpacing: 0.6, textTransform: 'uppercase', margin: '4px 0 12px' };

  return (
    <PhoneShell bg={T2.bg}>
      <MoreHeader title="메이트" />

      <div style={{ position: 'absolute', top: 108, left: 0, right: 0, bottom: 0, overflow: 'auto', padding: '4px 20px 40px' }}>
        {/* 친구 추가 — 검색 */}
        <div style={{
          display: 'flex', alignItems: 'center', gap: 9, marginTop: 4, marginBottom: 8,
          padding: '12px 14px', borderRadius: 12, background: '#fff', border: `1px solid ${T2.border}`,
        }}>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
            <circle cx="11" cy="11" r="7" stroke={T2.textMute} strokeWidth="1.8"/>
            <path d="M20 20l-3.2-3.2" stroke={T2.textMute} strokeWidth="1.8" strokeLinecap="round"/>
          </svg>
          <span style={{ fontSize: 14, color: T2.textMute, letterSpacing: -0.2 }}>이름으로 메이트 찾기</span>
        </div>

        {/* 내 메이트 */}
        <div style={labelStyle}>내 메이트 {myMates.length}</div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {myMates.map((m, i) => (
            <div key={i} style={{
              display: 'flex', alignItems: 'center', gap: 13, padding: 14,
              background: '#fff', borderRadius: 16, border: `1px solid ${T2.border}`, cursor: 'pointer',
            }}>
              <div style={{ position: 'relative', flexShrink: 0 }}>
                <div style={{
                  width: 48, height: 48, borderRadius: '50%',
                  background: T2.bg, border: `1px solid ${T2.border}`,
                  display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 23,
                }}>{m.emo}</div>
                {/* 혼밥 중 상태 점 */}
                {m.now && (
                  <div style={{
                    position: 'absolute', right: -1, bottom: -1, width: 15, height: 15, borderRadius: '50%',
                    background: '#22A65A', border: '2.5px solid #fff',
                  }} />
                )}
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <span style={{ fontSize: 15, fontWeight: 800, color: T2.text, letterSpacing: -0.3 }}>{m.name}</span>
                  {m.together > 0 && <span style={{ fontSize: 10, fontWeight: 700, color: T2.brand, background: T2.brandSoft, padding: '2px 6px', borderRadius: 5 }}>같이 {m.together}회</span>}
                </div>
                {/* 혼밥 중 / 메타 */}
                {m.now ? (
                  <div style={{ display: 'flex', alignItems: 'center', gap: 5, marginTop: 5 }}>
                    <span style={{ width: 6, height: 6, borderRadius: '50%', background: '#22A65A', flexShrink: 0 }} />
                    <span style={{ fontSize: 12, fontWeight: 700, color: '#22A65A', letterSpacing: -0.2 }}>지금 혼밥 중</span>
                    <span style={{ fontSize: 12, color: T2.textMute, letterSpacing: -0.2, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>· {m.nowPlace}</span>
                  </div>
                ) : (
                  <div style={{ fontSize: 12, color: T2.textMute, marginTop: 4 }}>{m.meta}</div>
                )}
                <div style={{ display: 'flex', gap: 5, marginTop: 8 }}>
                  {m.tags.map((t, k) => (
                    <span key={k} style={{ fontSize: 11, fontWeight: 600, color: T2.textSub, background: T2.bg, border: `1px solid ${T2.border}`, padding: '2px 7px', borderRadius: 6 }}>{t}</span>
                  ))}
                </div>
              </div>
              {/* 메이트 상태 */}
              <div style={{
                flexShrink: 0, alignSelf: 'flex-start', display: 'flex', alignItems: 'center', gap: 4,
                padding: '7px 11px', borderRadius: 9, fontSize: 12, fontWeight: 700, letterSpacing: -0.2,
                background: '#fff', color: T2.textSub, border: `1px solid ${T2.border}`,
              }}>
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none"><path d="M5 12.5l4.5 4.5L19 7" stroke={T2.textSub} strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"/></svg>
                메이트
              </div>
            </div>
          ))}
        </div>

        {/* 알 수도 있는 메이트 */}
        <div style={{ ...labelStyle, marginTop: 28 }}>알 수도 있는 메이트</div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {suggest.map((m, i) => {
            const sent = m.state === 'sent';
            return (
              <div key={i} style={{
                display: 'flex', alignItems: 'center', gap: 13, padding: 14,
                background: '#fff', borderRadius: 16, border: `1px solid ${T2.border}`,
              }}>
                <div style={{
                  width: 48, height: 48, borderRadius: '50%', flexShrink: 0,
                  background: T2.bg, border: `1px solid ${T2.border}`,
                  display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 23,
                }}>{m.emo}</div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 15, fontWeight: 800, color: T2.text, letterSpacing: -0.3 }}>{m.name}</div>
                  <div style={{ fontSize: 12, color: T2.textMute, marginTop: 4 }}>{m.meta}</div>
                  <div style={{ fontSize: 11, color: T2.brand, fontWeight: 600, marginTop: 6, letterSpacing: -0.2 }}>함께 아는 메이트 {m.mutual}명</div>
                </div>
                {/* 친구 추가 버튼 */}
                <div style={{
                  flexShrink: 0, alignSelf: 'flex-start', display: 'flex', alignItems: 'center', gap: 4,
                  padding: '7px 12px', borderRadius: 9, fontSize: 12, fontWeight: 700, letterSpacing: -0.2,
                  background: sent ? '#fff' : T2.brand,
                  color: sent ? T2.textMute : '#fff',
                  border: `1px solid ${sent ? T2.border : T2.brand}`, cursor: 'pointer',
                }}>
                  {sent ? '신청함' : (
                    <React.Fragment>
                      <span style={{ fontSize: 15, fontWeight: 400, lineHeight: 1, marginTop: -1 }}>+</span> 메이트 추가
                    </React.Fragment>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </PhoneShell>
  );
}

Object.assign(window, { Mates });
