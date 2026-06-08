// VerifyCode.jsx — 인증번호 입력 (02/03)
// (구 Min_Verify)

// ───────────────────────────────────────────────────────────
// 화면 B-2: 인증번호 입력 (02 / 03)
// ───────────────────────────────────────────────────────────
function VerifyCode() {
  const code = ['5', '2', '8', '0', '', ''];
  const filled = 4;
  return (
    <PhoneShell bg={T2.bg}>
      {/* 헤더 */}
      <div style={{
        position: 'absolute', top: 56, left: 0, right: 0, height: 56,
        display: 'flex', alignItems: 'center', padding: '0 20px', zIndex: 10,
      }}>
        <div style={{ fontSize: 20, color: T2.text }}>←</div>
      </div>

      <div style={{ position: 'absolute', top: 124, left: 0, right: 0, padding: '0 28px' }}>
        {/* 진행 — 02 / 03 */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 28 }}>
          <span style={{ fontSize: 12, fontWeight: 800, color: T2.text, fontFeatureSettings: '"tnum"' }}>02</span>
          <div style={{ flex: 1, height: 2, background: T2.border, position: 'relative' }}>
            <div style={{ position: 'absolute', left: 0, top: 0, height: '100%', width: '66%', background: T2.brand }} />
          </div>
          <span style={{ fontSize: 12, fontWeight: 600, color: T2.textMute, fontFeatureSettings: '"tnum"' }}>03</span>
        </div>

        <h1 style={{ fontSize: 30, fontWeight: 800, color: T2.text, letterSpacing: -1, margin: 0, lineHeight: 1.15 }}>
          인증번호를<br />입력해주세요
        </h1>
        <p style={{ fontSize: 14, color: T2.textSub, marginTop: 12, lineHeight: 1.5, letterSpacing: -0.3 }}>
          <b style={{ color: T2.text }}>+82 10 2580 ····</b> 로 보낸<br />6자리 숫자를 입력해주세요.
        </p>

        {/* 코드 입력 — 6칸 */}
        <div style={{ marginTop: 40, display: 'flex', gap: 9 }}>
          {code.map((c, i) => {
            const active = i === filled;
            const done = c !== '';
            return (
              <div key={i} style={{
                flex: 1, height: 62, borderRadius: 12,
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                fontSize: 26, fontWeight: 800, color: T2.text, letterSpacing: 0,
                fontFeatureSettings: '"tnum"',
                background: '#fff',
                border: `1.5px solid ${active ? T2.text : (done ? T2.borderStrong : T2.border)}`,
              }}>
                {done ? c : (active ? <span style={{ width: 2, height: 24, background: T2.brand }} /> : '')}
              </div>
            );
          })}
        </div>

        {/* 타이머 + 재전송 */}
        <div style={{ marginTop: 22, display: 'flex', alignItems: 'center', gap: 10 }}>
          <span style={{ fontSize: 13, fontWeight: 700, color: T2.brand, fontFeatureSettings: '"tnum"', letterSpacing: -0.2 }}>02:47</span>
          <span style={{ fontSize: 13, color: T2.textMute, letterSpacing: -0.2 }}>안에 입력해주세요</span>
          <span style={{ marginLeft: 'auto', fontSize: 13, fontWeight: 700, color: T2.textSub, letterSpacing: -0.2, textDecoration: 'underline', cursor: 'pointer' }}>재전송</span>
        </div>

        {/* 보조 안내 */}
        <div style={{
          marginTop: 28, padding: '13px 15px', borderRadius: 12,
          background: '#fff', border: `1px solid ${T2.border}`,
          display: 'flex', alignItems: 'center', gap: 10,
        }}>
          <svg width="17" height="17" viewBox="0 0 24 24" fill="none" style={{ flexShrink: 0 }}>
            <circle cx="12" cy="12" r="9" stroke={T2.textMute} strokeWidth="1.6"/>
            <path d="M12 8v5" stroke={T2.textMute} strokeWidth="1.8" strokeLinecap="round"/>
            <circle cx="12" cy="16" r="0.6" fill={T2.textMute} stroke={T2.textMute}/>
          </svg>
          <span style={{ fontSize: 12, color: T2.textSub, letterSpacing: -0.2, lineHeight: 1.4 }}>문자가 오지 않나요? 스팸함을 확인하거나 재전송해보세요.</span>
        </div>
      </div>

      {/* CTA */}
      <div style={{ position: 'absolute', bottom: 0, left: 0, right: 0, padding: '12px 24px 44px' }}>
        <div style={{
          padding: '16px', borderRadius: 12, background: T2.brand, color: '#fff',
          fontSize: 15, fontWeight: 700, textAlign: 'center', letterSpacing: -0.3,
        }}>인증 완료</div>
      </div>
    </PhoneShell>
  );
}

Object.assign(window, { VerifyCode });
