// ReceivedRequests.jsx — 받은 같이 먹기 신청
// (구 Min_Requests)

// ───────────────────────────────────────────────────────────
// 더보기 3: 받은 같이 먹기 신청
// ───────────────────────────────────────────────────────────
function ReceivedRequests() {
  const reqs = [
    { name: '점심혼밥러', emo: '🍙', place: '큰순두부 연남점', time: '오늘 12:30', meta: '혼밥 32회 · 같이 먹은 적 2회', msg: '저도 순두부 좋아해요! 같이 조용히 먹어요 :)', fresh: true },
    { name: '연남책방지기', emo: '📚', place: '혼밥의자', time: '오늘 13:00', meta: '혼밥 12회 · 첫 매칭', msg: '바테이블 옆자리 어떠세요?', fresh: true },
  ];
  const past = [
    { name: '조용한미식가', emo: '🍜', place: '옥상국밥', state: '함께 먹음' },
    { name: '국밥러버', emo: '🍲', place: '큰순두부 연남점', state: '지난 신청' },
  ];

  return (
    <PhoneShell bg={T2.bg}>
      <MoreHeader title="받은 같이 먹기 신청" />

      <div style={{ position: 'absolute', top: 108, left: 0, right: 0, bottom: 0, overflow: 'auto', padding: '8px 20px 40px' }}>
        {/* 새 신청 */}
        <div style={{ fontSize: 11, fontWeight: 700, color: T2.textMute, letterSpacing: 0.6, textTransform: 'uppercase', margin: '4px 0 12px' }}>
          새로운 신청 {reqs.length}
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          {reqs.map((r, i) => (
            <div key={i} style={{ padding: 18, background: '#fff', borderRadius: 18, border: `1px solid ${T2.border}` }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                <div style={{
                  width: 46, height: 46, borderRadius: '50%', flexShrink: 0,
                  background: T2.bg, border: `1px solid ${T2.border}`,
                  display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 22,
                }}>{r.emo}</div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 15, fontWeight: 800, color: T2.text, letterSpacing: -0.3 }}>{r.name}</div>
                  <div style={{ fontSize: 11, color: T2.textMute, marginTop: 3 }}>{r.meta}</div>
                </div>
                <span style={{ fontSize: 11, color: T2.textMute, flexShrink: 0 }}>{r.time}</span>
              </div>

              {/* 장소 + 메시지 */}
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

              {/* 버튼 */}
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

        {/* 지난 신청 */}
        <div style={{ fontSize: 11, fontWeight: 700, color: T2.textMute, letterSpacing: 0.6, textTransform: 'uppercase', margin: '28px 0 4px' }}>
          지난 신청
        </div>
        <div>
          {past.map((p, i) => (
            <div key={i} style={{
              display: 'flex', alignItems: 'center', gap: 12, padding: '14px 0',
              borderBottom: i < past.length - 1 ? `1px solid ${T2.border}` : 'none',
            }}>
              <div style={{
                width: 40, height: 40, borderRadius: '50%', flexShrink: 0,
                background: T2.bg, border: `1px solid ${T2.border}`,
                display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 19, opacity: 0.7,
              }}>{p.emo}</div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 14, fontWeight: 700, color: T2.textSub, letterSpacing: -0.3 }}>{p.name}</div>
                <div style={{ fontSize: 11, color: T2.textMute, marginTop: 3 }}>{p.place}</div>
              </div>
              <span style={{ fontSize: 12, fontWeight: 600, color: T2.textMute }}>{p.state}</span>
            </div>
          ))}
        </div>
      </div>
    </PhoneShell>
  );
}

Object.assign(window, { ReceivedRequests });
