// TogetherFeed.jsx — 같이 먹기 (하단바 신규 탭)
// 핵심 액션 화면: 받은/보낸 신청 + 지금 같이 먹을 수 있는 사람

// ───────────────────────────────────────────────────────────
// 하단바 2: 같이 먹기
// ───────────────────────────────────────────────────────────
function TogetherFeed() {
  const [tab, setTab] = React.useState('received');

  const received = [
    { name: '점심혼밥러', emo: '🍙', place: '큰순두부 연남점', time: '방금', meta: '혼밥 32회 · 같이 먹은 적 2회', msg: '저도 순두부 좋아해요! 같이 조용히 먹어요 :)', mate: true },
    { name: '연남책방지기', emo: '📚', place: '혼밥의자', time: '12분 전', meta: '혼밥 12회 · 첫 매칭', msg: '바테이블 옆자리 어떠세요?', mate: false },
  ];
  const sent = [
    { name: '조용한미식가', emo: '🍜', place: '옥상국밥', time: '오늘 12:10', state: 'accepted' },
    { name: '국밥러버', emo: '🍲', place: '큰순두부 연남점', time: '어제', state: 'pending' },
    { name: '면식수행', emo: '🍝', place: '연남 파스타바', time: '2일 전', state: 'declined' },
  ];
  const liveNow = [
    { name: '혼밥부장', emo: '🍱', place: '큰순두부 연남점', dist: '120m', since: '8분째', mood: '대화 환영', mate: true },
    { name: '도시락주의', emo: '🥡', place: '옥상국밥', dist: '480m', since: '3분째', mood: '조용히', mate: false },
    { name: '연남또일이', emo: '🍳', place: '혼밥의자', dist: '650m', since: '15분째', mood: '대화 환영', mate: false },
  ];

  const stateMap = {
    accepted: { label: '수락됨 · 약속 잡기', color: T2.brand, bg: T2.brandSoft, strong: true },
    pending: { label: '응답 대기 중', color: T2.textMute, bg: T2.bg, strong: false },
    declined: { label: '거절됨', color: T2.textMute, bg: T2.bg, strong: false },
  };

  return (
    <PhoneShell bg={T2.bg}>
      {/* 헤더 */}
      <div style={{ position: 'absolute', top: 60, left: 0, right: 0, padding: '0 20px', zIndex: 10, background: T2.bg }}>
        <h1 style={{ fontSize: 28, fontWeight: 800, color: T2.text, letterSpacing: -1, margin: 0 }}>같이 먹기</h1>

        {/* 세그먼트 */}
        <div style={{ display: 'flex', gap: 22, marginTop: 16 }}>
          {[
            { key: 'received', label: '받은 신청', count: received.length },
            { key: 'sent', label: '보낸 신청', count: null },
          ].map((s) => {
            const on = tab === s.key;
            return (
              <div key={s.key} onClick={() => setTab(s.key)} style={{
                position: 'relative', paddingBottom: 12, display: 'flex', alignItems: 'center', gap: 6, cursor: 'pointer',
              }}>
                <span style={{ fontSize: 16, fontWeight: 800, color: on ? T2.text : T2.textMute, letterSpacing: -0.3 }}>{s.label}</span>
                {s.count != null && <span style={{ fontSize: 12, fontWeight: 700, color: on ? T2.brand : T2.textMute, fontFeatureSettings: '"tnum"' }}>{s.count}</span>}
                {on && <div style={{ position: 'absolute', left: 0, right: 0, bottom: 0, height: 2, background: T2.brand }} />}
              </div>
            );
          })}
        </div>
        <div style={{ height: 1, background: T2.border, marginTop: -1 }} />
      </div>

      {/* 본문 */}
      <div style={{ position: 'absolute', top: 158, left: 0, right: 0, bottom: 92, overflow: 'auto', padding: '16px 20px 32px' }}>

        {/* 지금 같이 먹을 수 있어요 — 항상 상단 노출 */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 7 }}>
            <span style={{ width: 7, height: 7, borderRadius: '50%', background: T2.brand, boxShadow: '0 0 0 4px rgba(255,90,31,0.15)' }} />
            <span style={{ fontSize: 11, fontWeight: 700, color: T2.text, letterSpacing: 0.6, textTransform: 'uppercase' }}>지금 같이 먹을 수 있어요</span>
          </div>
          <span style={{ fontSize: 12, fontWeight: 700, color: T2.textMute, fontFeatureSettings: '"tnum"' }}>내 주변 {liveNow.length}</span>
        </div>
        <div style={{ display: 'flex', gap: 10, overflowX: 'auto', margin: '0 -20px', padding: '0 20px 4px', scrollbarWidth: 'none' }}>
          {liveNow.map((p, i) => (
            <div key={i} style={{
              width: 156, flexShrink: 0, padding: 14, background: '#fff',
              borderRadius: 16, border: `1px solid ${T2.border}`, boxShadow: '0 1px 4px rgba(0,0,0,0.04)',
            }}>
              <div style={{ position: 'relative', width: 44, height: 44 }}>
                <div style={{
                  width: 44, height: 44, borderRadius: '50%',
                  background: T2.bg, border: `1px solid ${T2.border}`,
                  display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 21,
                }}>{p.emo}</div>
                <span style={{ position: 'absolute', right: -1, bottom: -1, width: 12, height: 12, borderRadius: '50%', background: T2.brand, border: '2.5px solid #fff' }} />
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 5, marginTop: 11 }}>
                <span style={{ fontSize: 14, fontWeight: 800, color: T2.text, letterSpacing: -0.3, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{p.name}</span>
                {p.mate && <span style={{ fontSize: 9, fontWeight: 800, color: T2.brand, flexShrink: 0 }}>메이트</span>}
              </div>
              <div style={{ fontSize: 11.5, color: T2.textSub, marginTop: 4, letterSpacing: -0.2, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{p.place}</div>
              <div style={{ fontSize: 11, color: T2.textMute, marginTop: 2, fontFeatureSettings: '"tnum"' }}>{p.dist} · {p.since}</div>
              <div style={{
                marginTop: 11, textAlign: 'center', padding: '8px 0', borderRadius: 9,
                background: T2.brand, color: '#fff', fontSize: 12.5, fontWeight: 700, letterSpacing: -0.3, cursor: 'pointer',
              }}>같이 먹기</div>
            </div>
          ))}
        </div>

        <div style={{ height: 1, background: T2.border, margin: '22px 0 18px' }} />

        {/* 받은 신청 */}
        {tab === 'received' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {received.map((r, i) => (
              <div key={i} style={{ padding: 18, background: '#fff', borderRadius: 18, border: `1px solid ${T2.border}` }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                  <div style={{
                    width: 46, height: 46, borderRadius: '50%', flexShrink: 0,
                    background: T2.bg, border: `1px solid ${T2.border}`,
                    display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 22,
                  }}>{r.emo}</div>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                      <span style={{ fontSize: 15, fontWeight: 800, color: T2.text, letterSpacing: -0.3 }}>{r.name}</span>
                      {r.mate && <span style={{ fontSize: 10, fontWeight: 700, color: T2.brand, background: T2.brandSoft, padding: '2px 6px', borderRadius: 5 }}>메이트</span>}
                    </div>
                    <div style={{ fontSize: 11, color: T2.textMute, marginTop: 3 }}>{r.meta}</div>
                  </div>
                  <span style={{ fontSize: 11, color: T2.textMute, flexShrink: 0 }}>{r.time}</span>
                </div>

                <div style={{
                  display: 'flex', alignItems: 'center', gap: 7, marginTop: 14,
                  padding: '8px 12px', borderRadius: 10, background: T2.brandSoft,
                }}>
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                    <path d="M12 2C8.1 2 5 5.1 5 9c0 5.2 7 13 7 13s7-7.8 7-13c0-3.9-3.1-7-7-7z" stroke={T2.brand} strokeWidth="1.7" strokeLinejoin="round"/>
                    <circle cx="12" cy="9" r="2.2" stroke={T2.brand} strokeWidth="1.7"/>
                  </svg>
                  <span style={{ fontSize: 12, fontWeight: 700, color: T2.brand, letterSpacing: -0.2 }}>{r.place}</span>
                </div>
                <div style={{ fontSize: 13, color: T2.textSub, lineHeight: 1.6, marginTop: 12, letterSpacing: -0.3 }}>"{r.msg}"</div>

                <div style={{ display: 'flex', gap: 8, marginTop: 16 }}>
                  <div style={{
                    flex: 1, textAlign: 'center', padding: '13px', borderRadius: 11,
                    background: T2.bg, color: T2.textSub, fontSize: 14, fontWeight: 700, letterSpacing: -0.3, cursor: 'pointer',
                  }}>거절</div>
                  <div style={{
                    flex: 2, textAlign: 'center', padding: '13px', borderRadius: 11,
                    background: T2.brand, color: '#fff', fontSize: 14, fontWeight: 700, letterSpacing: -0.3, cursor: 'pointer',
                  }}>수락하기</div>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* 보낸 신청 */}
        {tab === 'sent' && (
          <div>
            {sent.map((s, i) => {
              const st = stateMap[s.state];
              return (
                <div key={i} style={{
                  display: 'flex', alignItems: 'center', gap: 12, padding: '15px 0',
                  borderBottom: i < sent.length - 1 ? `1px solid ${T2.border}` : 'none',
                }}>
                  <div style={{
                    width: 44, height: 44, borderRadius: '50%', flexShrink: 0,
                    background: T2.bg, border: `1px solid ${T2.border}`,
                    display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 20,
                    opacity: s.state === 'declined' ? 0.55 : 1,
                  }}>{s.emo}</div>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: 14.5, fontWeight: 700, color: s.state === 'declined' ? T2.textMute : T2.text, letterSpacing: -0.3 }}>{s.name}</div>
                    <div style={{ fontSize: 12, color: T2.textMute, marginTop: 3, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{s.place} · {s.time}</div>
                  </div>
                  <span style={{
                    flexShrink: 0, padding: '7px 12px', borderRadius: 999,
                    background: st.bg, color: st.color, fontSize: 12, fontWeight: 700, letterSpacing: -0.2,
                    border: st.strong ? `1px solid rgba(255,90,31,0.2)` : `1px solid ${T2.border}`, cursor: st.strong ? 'pointer' : 'default', whiteSpace: 'nowrap',
                  }}>{st.label}</span>
                </div>
              );
            })}
          </div>
        )}
      </div>

      <MinTabBar active="meal" />
    </PhoneShell>
  );
}

Object.assign(window, { TogetherFeed });
