// MealRequest.jsx — 같이 먹기 신청
// (구 Min_MealRequest)

// ───────────────────────────────────────────────────────────
// 화면: 같이 먹기 신청 (식당 상세 → 인사/신청 버튼 진입)
// ───────────────────────────────────────────────────────────
function MealRequest() {
  const people = [
    { name: '점심혼밥러', emo: '🍙', meta: '혼밥 32회 · 대화 OK', mate: true },
    { name: '조용한미식가', emo: '🍜', meta: '혼밥 18회 · 조용히', mate: false },
  ];
  const sel = 0;
  const quick = ['조용히 각자 먹어요 :)', '가볍게 대화 나눠요', '혼밥 입문이에요, 잘 부탁해요'];
  const quickSel = 0;

  return (
    <PhoneShell bg={T2.bg}>
      {/* 상단 바 */}
      <div style={{
        position: 'absolute', top: 54, left: 0, right: 0, height: 52, zIndex: 10,
        display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 20px',
      }}>
        <span style={{ fontSize: 15, fontWeight: 600, color: T2.textSub, letterSpacing: -0.3, cursor: 'pointer' }}>취소</span>
        <span style={{ fontSize: 16, fontWeight: 800, color: T2.text, letterSpacing: -0.3 }}>같이 먹기 신청</span>
        <span style={{ width: 28 }} />
      </div>

      <div style={{ position: 'absolute', top: 118, left: 0, right: 0, bottom: 96, overflow: 'auto', padding: '4px 20px 24px' }}>
        {/* 식당 요약 */}
        <div style={{
          display: 'flex', alignItems: 'center', gap: 13, padding: 14, marginTop: 4,
          background: '#fff', borderRadius: 16, border: `1px solid ${T2.border}`,
        }}>
          <div style={{
            width: 50, height: 50, borderRadius: 12, flexShrink: 0,
            background: T2.mapBg, border: `1px solid ${T2.border}`,
            display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 22,
          }}>🍲</div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontSize: 15, fontWeight: 800, color: T2.text, letterSpacing: -0.3 }}>큰순두부 연남점</div>
            <div style={{ fontSize: 12, color: T2.textSub, marginTop: 4 }}>한식 · 120m · 마포구 성미산로 161-4</div>
          </div>
        </div>

        {/* 누구에게 */}
        <div style={{ marginTop: 26 }}>
          <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between' }}>
            <span style={{ fontSize: 12, fontWeight: 700, color: T2.textMute, letterSpacing: 0.5, textTransform: 'uppercase' }}>누구에게</span>
            <span style={{ fontSize: 11, color: T2.brand, fontWeight: 700, letterSpacing: -0.2 }}>지금 혼밥 중 {people.length}명</span>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10, marginTop: 12 }}>
            {people.map((p, i) => {
              const on = i === sel;
              return (
                <div key={i} style={{
                  display: 'flex', alignItems: 'center', gap: 12, padding: 14, borderRadius: 14,
                  background: '#fff', cursor: 'pointer',
                  border: `1.5px solid ${on ? T2.brand : T2.border}`,
                }}>
                  <div style={{
                    width: 44, height: 44, borderRadius: '50%', flexShrink: 0,
                    background: T2.bg, border: `1px solid ${T2.border}`,
                    display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 21,
                  }}>{p.emo}</div>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                      <span style={{ fontSize: 15, fontWeight: 800, color: T2.text, letterSpacing: -0.3 }}>{p.name}</span>
                      {p.mate && <span style={{ fontSize: 10, fontWeight: 700, color: T2.brand, background: T2.brandSoft, padding: '2px 6px', borderRadius: 5 }}>메이트</span>}
                    </div>
                    <div style={{ fontSize: 12, color: T2.textMute, marginTop: 4 }}>{p.meta}</div>
                  </div>
                  {/* 선택 표시 */}
                  <div style={{
                    width: 24, height: 24, borderRadius: '50%', flexShrink: 0,
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    background: on ? T2.brand : '#fff',
                    border: `1.5px solid ${on ? T2.brand : T2.borderStrong}`,
                  }}>
                    {on && <svg width="14" height="14" viewBox="0 0 24 24" fill="none"><path d="M5 12.5l4.5 4.5L19 7" stroke="#fff" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round"/></svg>}
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* 인사 한마디 */}
        <div style={{ marginTop: 26 }}>
          <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between' }}>
            <span style={{ fontSize: 12, fontWeight: 700, color: T2.textMute, letterSpacing: 0.5, textTransform: 'uppercase' }}>인사 한마디</span>
            <span style={{ fontSize: 11, color: T2.textMute, fontFeatureSettings: '"tnum"' }}>14 / 40</span>
          </div>
          {/* 입력 */}
          <div style={{
            marginTop: 12, padding: '14px 16px', minHeight: 64, borderRadius: 14,
            background: '#fff', border: `1.5px solid ${T2.text}`,
            fontSize: 15, color: T2.text, lineHeight: 1.5, letterSpacing: -0.3,
          }}>
            {quick[quickSel]}
            <span style={{ display: 'inline-block', width: 1.5, height: 17, background: T2.brand, verticalAlign: 'text-bottom', marginLeft: 1 }} />
          </div>
          {/* 빠른 문구 */}
          <div style={{ display: 'flex', gap: 7, flexWrap: 'wrap', marginTop: 12 }}>
            {quick.map((q, i) => (
              <div key={i} style={{
                padding: '8px 13px', borderRadius: 999, fontSize: 13, fontWeight: 600, letterSpacing: -0.3, cursor: 'pointer',
                background: i === quickSel ? T2.brandSoft : '#fff',
                color: i === quickSel ? T2.brand : T2.textSub,
                border: `1px solid ${i === quickSel ? 'rgba(255,90,31,0.2)' : T2.border}`,
              }}>{q}</div>
            ))}
          </div>
        </div>
      </div>

      {/* 하단 고정 — 보내기 */}
      <div style={{
        position: 'absolute', left: 0, right: 0, bottom: 0, zIndex: 20,
        padding: '12px 16px 40px', background: '#fff', borderTop: `1px solid ${T2.border}`,
      }}>
        <div style={{
          padding: '16px', borderRadius: 12, background: T2.brand, color: '#fff',
          fontSize: 15, fontWeight: 800, letterSpacing: -0.3, textAlign: 'center',
          boxShadow: '0 6px 18px rgba(255,90,31,0.28)',
        }}>같이 먹기 신청 보내기</div>
      </div>
    </PhoneShell>
  );
}

Object.assign(window, { MealRequest });
