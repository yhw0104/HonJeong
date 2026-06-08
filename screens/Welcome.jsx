// Welcome.jsx — 웰컴 / 로그인 진입
// (구 Min_Welcome)

// direction2-minimal-login.jsx — 미니멀 모던 톤의 로그인 / 온보딩 플로우
// T2: 모노톤 + 절제된 오렌지 한 점. 여백·타이포 중심.

// ───────────────────────────────────────────────────────────
// 화면 A: 웰컴 / 로그인 진입
// ───────────────────────────────────────────────────────────
function Welcome() {
  return (
    <PhoneShell bg={T2.bg}>
      {/* 메인 타이포 블록 */}
      <div style={{ position: 'absolute', top: 200, left: 28, right: 28, zIndex: 10 }}>
        <div style={{ fontSize: 12, fontWeight: 700, color: T2.textMute, letterSpacing: 1, textTransform: 'uppercase' }}>
          혼밥을 정상화하다
        </div>
        <h1 style={{
          fontSize: 40, fontWeight: 800, color: T2.text, letterSpacing: -1.4,
          margin: '14px 0 0', lineHeight: 1.12
        }}>
          혼자 밥 먹는 게<br />
          <span style={{ color: T2.brand }}>쉬워질 때</span>까지
        </h1>
        <p style={{
          fontSize: 15, color: T2.textSub, lineHeight: 1.6, marginTop: 18, letterSpacing: -0.3,
          maxWidth: 300, textWrap: 'pretty'
        }}>
          혼자여도 괜찮은 식당, 그리고 같은 시간 같은 자리의 사람들. 오늘 한 끼, 편하게 누려보세요.
        </p>
      </div>

      {/* 인디케이터 — 지금 혼밥 중 (심플 & 눈에 띄게) */}
      <div style={{
        position: 'absolute', left: 28, right: 28, bottom: 332, zIndex: 10
      }}>
        {/* 윗줄: 펄스 도트 + 지금 이 순간 */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <div style={{ position: 'relative', width: 10, height: 10, flexShrink: 0 }}>
            <div style={{ position: 'absolute', inset: -4, borderRadius: '50%', background: T2.brand, opacity: 0.18 }} />
            <div style={{ position: 'absolute', inset: 0, borderRadius: '50%', background: T2.brand }} />
          </div>
          <span style={{ fontSize: 15, fontWeight: 700, color: T2.textSub, letterSpacing: -0.3 }}>지금 이 순간,</span>
        </div>
        {/* 아랫줄: 27 강조 + 명의 혼밥러들 */}
        <div style={{ display: 'flex', alignItems: 'baseline', gap: 6, marginTop: 8, width: "332px", paddingLeft: 18 }}>
          <span style={{ fontSize: 56, fontWeight: 800, color: T2.brand, letterSpacing: -3, lineHeight: 1, fontFeatureSettings: '"tnum"', width: "69px", marginLeft: -3 }}>27</span>
          <span style={{ fontWeight: 800, color: T2.text, letterSpacing: -1, lineHeight: 1, fontSize: "21px" }}>명의 혼밥러들</span>
        </div>
      </div>

      {/* 하단 CTA */}
      <div style={{ position: 'absolute', bottom: 0, left: 0, right: 0, padding: '16px 24px 40px' }}>
        {/* 카카오 — 대표 소셜 로그인 */}
        <div style={{ padding: '15px', borderRadius: 12, background: '#FEE500', color: '#191600',
          fontSize: 15, fontWeight: 700, letterSpacing: -0.3, whiteSpace: 'nowrap',
          display: 'flex', alignItems: 'center', justifyContent: 'center'
        }}>
          <span style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}>
            <svg width="18" height="18" viewBox="0 0 18 18" fill="none" style={{ flexShrink: 0 }}>
              <path d="M9 1.6C4.8 1.6 1.4 4.3 1.4 7.6c0 2.1 1.4 4 3.6 5.1-.16.57-.58 2.06-.66 2.38-.1.4.14.4.3.29.13-.09 2.05-1.39 2.88-1.96.48.07.97.1 1.48.1 4.2 0 7.6-2.7 7.6-6C16.6 4.3 13.2 1.6 9 1.6z" fill="#191600" />
            </svg> 카카오로 계속하기
          </span>
        </div>
        {/* Apple */}
        <div style={{
          marginTop: 8, padding: '15px', borderRadius: 12, background: T2.text, color: '#fff',
          fontSize: 15, fontWeight: 700, letterSpacing: -0.3, whiteSpace: 'nowrap',
          display: 'flex', alignItems: 'center', justifyContent: 'center'
        }}>
          <span style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}>
            <svg width="16" height="18" viewBox="0 0 24 24" fill="#fff" style={{ flexShrink: 0, marginTop: -2 }}>
              <path d="M17.05 12.04c-.03-2.6 2.12-3.85 2.22-3.91-1.21-1.77-3.09-2.01-3.76-2.04-1.6-.16-3.12.94-3.93.94-.81 0-2.06-.92-3.39-.89-1.74.03-3.35 1.01-4.25 2.57-1.81 3.15-.46 7.81 1.3 10.37.86 1.25 1.89 2.66 3.23 2.61 1.3-.05 1.79-.84 3.36-.84 1.57 0 2.01.84 3.39.81 1.4-.02 2.29-1.28 3.15-2.54.99-1.46 1.4-2.87 1.42-2.94-.03-.01-2.72-1.04-2.75-4.13zM14.6 4.56c.72-.87 1.2-2.08 1.07-3.28-1.03.04-2.28.69-3.02 1.56-.66.77-1.24 2-1.08 3.18 1.15.09 2.32-.59 3.03-1.46z" />
            </svg> Apple로 계속하기
          </span>
        </div>
        {/* 휴대폰 — 브랜드 컬러 버튼 */}
        <div style={{
          marginTop: 8, padding: '15px', borderRadius: 12, background: T2.brand, color: '#fff',
          fontSize: 15, fontWeight: 700, letterSpacing: -0.3, whiteSpace: 'nowrap',
          display: 'flex', alignItems: 'center', justifyContent: 'center'
        }}>
          <span style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}>
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none" style={{ flexShrink: 0, marginLeft: 1 }}>
              <rect x="4" y="1" width="8" height="14" rx="2" stroke="#fff" strokeWidth="1.5" />
              <line x1="6.8" y1="12.6" x2="9.2" y2="12.6" stroke="#fff" strokeWidth="1.5" strokeLinecap="round" />
            </svg> 휴대폰 번호로 계속하기
          </span>
        </div>
      </div>
    </PhoneShell>);

}

Object.assign(window, { Welcome });
