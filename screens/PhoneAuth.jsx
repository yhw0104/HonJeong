// PhoneAuth.jsx — 휴대폰 번호 인증 (01/03)
// (구 Min_Login)

// ───────────────────────────────────────────────────────────
// 화면 B: 휴대폰 번호 인증
// ───────────────────────────────────────────────────────────
function PhoneAuth() {
  return (
    <PhoneShell bg={T2.bg}>
      {/* 헤더 */}
      <div style={{
        position: 'absolute', top: 56, left: 0, right: 0, height: 56,
        display: 'flex', alignItems: 'center', padding: '0 20px', zIndex: 10
      }}>
        <div style={{ fontSize: 20, color: T2.text }}>←</div>
      </div>

      <div style={{ position: 'absolute', top: 124, left: 0, right: 0, padding: '0 28px' }}>
        {/* 진행 — 미니멀 점/숫자 */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 28 }}>
          <span style={{ fontSize: 12, fontWeight: 800, color: T2.text, fontFeatureSettings: '"tnum"' }}>01</span>
          <div style={{ flex: 1, height: 2, background: T2.border, position: 'relative' }}>
            <div style={{ position: 'absolute', left: 0, top: 0, height: '100%', width: '33%', background: T2.brand }} />
          </div>
          <span style={{ fontSize: 12, fontWeight: 600, color: T2.textMute, fontFeatureSettings: '"tnum"' }}>03</span>
        </div>

        <h1 style={{ fontSize: 30, fontWeight: 800, color: T2.text, letterSpacing: -1, margin: 0, lineHeight: 1.15 }}>
          휴대폰 번호를<br />입력해주세요
        </h1>
        <p style={{ fontSize: 14, color: T2.textSub, marginTop: 12, lineHeight: 1.5, letterSpacing: -0.3 }}>
          인증 후 같은 동네 혼밥 친구를 안전하게 만나요. 번호는 공개되지 않습니다.
        </p>

        {/* 입력 */}
        <div style={{ marginTop: 40 }}>
          <div style={{ fontSize: 12, fontWeight: 700, color: T2.textMute, letterSpacing: 0.5, textTransform: 'uppercase', marginBottom: 10 }}>
            휴대폰 번호
          </div>
          <div style={{
            display: 'flex', alignItems: 'center', gap: 12,
            paddingBottom: 14, borderBottom: `2px solid ${T2.text}`
          }}>
            <span style={{ fontSize: 22, fontWeight: 700, color: T2.textMute, letterSpacing: -0.5 }}>+82</span>
            <div style={{ flex: 1, fontSize: 22, fontWeight: 700, color: T2.text, letterSpacing: 0.5, fontFeatureSettings: '"tnum"' }}>
              10 2580 ····
              <span style={{ display: 'inline-block', width: 2, height: 22, background: T2.brand, marginLeft: 3, verticalAlign: 'text-bottom' }} />
            </div>
          </div>
        </div>

        {/* 약관 — 미니멀 라인 */}
        <div style={{ marginTop: 32 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, paddingBottom: 14, borderBottom: `1px solid ${T2.border}` }}>
            <div style={{
              width: 20, height: 20, borderRadius: '50%', background: T2.text,
              display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff', fontSize: 11, fontWeight: 800
            }}>✓</div>
            <div style={{ flex: 1, fontSize: 14, fontWeight: 700, color: T2.text, letterSpacing: -0.3 }}>약관에 모두 동의</div>
          </div>
          {[
          { l: '서비스 이용약관', req: true },
          { l: '개인정보 처리방침', req: true },
          { l: '위치정보 이용 동의', req: true },
          { l: '마케팅 알림 수신', req: false, off: true }].
          map((r, i) =>
          <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '11px 0' }}>
              <div style={{
              width: 16, height: 16, borderRadius: '50%',
              background: r.off ? 'transparent' : T2.brand,
              border: r.off ? `1.5px solid ${T2.borderStrong}` : 'none',
              display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff', fontSize: 9, fontWeight: 800
            }}>{r.off ? '' : '✓'}</div>
              <div style={{ flex: 1, fontSize: 13, color: r.off ? T2.textMute : T2.textSub, letterSpacing: -0.2 }}>
                <span style={{ fontWeight: 700, color: r.req ? T2.text : T2.textMute, marginRight: 5 }}>{r.req ? '필수' : '선택'}</span>{r.l}
              </div>
              <span style={{ fontSize: 13, color: T2.textMute }}>›</span>
            </div>
          )}
        </div>
      </div>

      {/* CTA */}
      <div style={{ position: 'absolute', bottom: 0, left: 0, right: 0, padding: '12px 24px 44px' }}>
        <div style={{
          padding: '16px', borderRadius: 12, background: T2.brand, color: '#fff',
          fontSize: 15, fontWeight: 700, textAlign: 'center', letterSpacing: -0.3
        }}>인증번호 받기</div>
      </div>
    </PhoneShell>);

}

Object.assign(window, { PhoneAuth });
