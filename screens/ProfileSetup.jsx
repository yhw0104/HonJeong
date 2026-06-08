// ProfileSetup.jsx — 닉네임 + 동네 설정 (03/03)
// (구 Min_Nickname)

// ───────────────────────────────────────────────────────────
// 화면 C: 닉네임 + 동네 + 혼밥 레벨
// ───────────────────────────────────────────────────────────
function ProfileSetup() {
  return (
    <PhoneShell bg={T2.bg}>
      {/* 헤더 */}
      <div style={{
        position: 'absolute', top: 56, left: 0, right: 0, height: 56,
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        padding: '0 20px', zIndex: 10
      }}>
        <div style={{ fontSize: 20, color: T2.text }}>←</div>
      </div>

      <div style={{ position: 'absolute', top: 124, left: 0, right: 0, bottom: 108, overflow: 'auto', padding: '0 28px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 28 }}>
          <span style={{ fontSize: 12, fontWeight: 800, color: T2.text, fontFeatureSettings: '"tnum"' }}>03</span>
          <div style={{ flex: 1, height: 2, background: T2.border, position: 'relative' }}>
            <div style={{ position: 'absolute', left: 0, top: 0, height: '100%', width: '100%', background: T2.brand }} />
          </div>
          <span style={{ fontSize: 12, fontWeight: 800, color: T2.brand, fontFeatureSettings: '"tnum"' }}>03</span>
        </div>

        <h1 style={{ fontSize: 30, fontWeight: 800, color: T2.text, letterSpacing: -1, margin: 0, lineHeight: 1.15 }}>
          프로필을<br />완성해주세요
        </h1>
        <p style={{ fontSize: 14, color: T2.textSub, marginTop: 12, lineHeight: 1.5, letterSpacing: -0.3 }}>
          <b style={{ color: T2.text }}>같이 먹기</b>를 신청하거나 받을 때, 상대에게 보여지는 정보예요.
        </p>

        {/* 프로필 사진 */}
        <div style={{ marginTop: 28, display: 'flex', alignItems: 'center', gap: 16 }}>
          <div style={{ position: 'relative' }}>
            <div style={{
              width: 72, height: 72, borderRadius: '50%', background: T2.text,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              color: '#fff', fontSize: 28, fontWeight: 800
            }}>혜</div>
            <div style={{
              position: 'absolute', right: -2, bottom: -2, width: 26, height: 26, borderRadius: '50%',
              background: T2.brand, border: '2.5px solid ' + T2.bg,
              display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 12
            }}>📷</div>
          </div>
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 14, fontWeight: 700, color: T2.text, letterSpacing: -0.3 }}>프로필 사진 추가</div>
            <div style={{ fontSize: 12, color: T2.textMute, marginTop: 2, lineHeight: 1.4 }}>상대에게 보여지는 정보예요.<br />얼굴 사진이면 더 좋아요.</div>
          </div>
        </div>

        {/* 닉네임 */}
        <div style={{ marginTop: 32 }}>
          <div style={{ fontSize: 12, fontWeight: 700, color: T2.textMute, letterSpacing: 0.5, textTransform: 'uppercase', marginBottom: 10 }}>
            닉네임
          </div>
          <div style={{
            display: 'flex', alignItems: 'center', gap: 10,
            paddingBottom: 12, borderBottom: `2px solid ${T2.text}`
          }}>
            <div style={{ flex: 1, fontSize: 20, fontWeight: 700, color: T2.text, letterSpacing: -0.4 }}>
              혜린
              <span style={{ display: 'inline-block', width: 2, height: 20, background: T2.brand, marginLeft: 2, verticalAlign: 'text-bottom' }} />
            </div>
            <span style={{ fontSize: 12, fontWeight: 700, color: T2.brand }}>사용 가능</span>
          </div>
          <div style={{ marginTop: 8, fontSize: 11, color: T2.textMute }}>한글·영문 2–10자 · 언제든 변경 가능</div>
        </div>

        {/* 성별 · 연령대 */}
        <div style={{ marginTop: 28, display: 'flex', gap: 20 }}>
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 12, fontWeight: 700, color: T2.textMute, letterSpacing: 0.5, textTransform: 'uppercase', marginBottom: 10 }}>성별</div>
            <div style={{ display: 'flex', gap: 6 }}>
              {[{ l: '여성', on: true }, { l: '남성', on: false }].map((g, i) =>
              <div key={i} style={{
                flex: 1, padding: '11px 0', borderRadius: 10, textAlign: 'center',
                fontSize: 14, fontWeight: 700, letterSpacing: -0.3,
                background: g.on ? T2.text : '#fff', color: g.on ? '#fff' : T2.textMute,
                border: `1px solid ${g.on ? T2.text : T2.border}`
              }}>{g.l}</div>
              )}
            </div>
          </div>
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 12, fontWeight: 700, color: T2.textMute, letterSpacing: 0.5, textTransform: 'uppercase', marginBottom: 10 }}>연령대</div>
            <div style={{
              display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '11px 14px',
              borderRadius: 10, background: '#fff', border: `1px solid ${T2.border}`
            }}>
              <span style={{ fontSize: 14, fontWeight: 700, color: T2.text, letterSpacing: -0.3 }}>20대</span>
              <svg width="11" height="11" viewBox="0 0 11 11" fill="none" stroke={T2.textMute} strokeWidth="1.6" strokeLinecap="round"><path d="M2 4l3.5 3.5L9 4" /></svg>
            </div>
          </div>
        </div>

        {/* 한 줄 소개 */}
        <div style={{ marginTop: 28 }}>
          <div style={{ fontSize: 12, fontWeight: 700, color: T2.textMute, letterSpacing: 0.5, textTransform: 'uppercase', marginBottom: 10 }}>
            한 줄 소개
          </div>
          <div style={{
            padding: '14px 16px', borderRadius: 12, background: '#fff', border: `1px solid ${T2.border}`,
            fontSize: 14, color: T2.text, lineHeight: 1.5, letterSpacing: -0.3, minHeight: 44
          }}>
            조용히 먹는 것도, 도란도란 얘기하는 것도 좋아요.
            <span style={{ display: 'inline-block', width: 2, height: 16, background: T2.brand, marginLeft: 1, verticalAlign: 'text-bottom' }} />
          </div>
        </div>

        {/* 좋아하는 음식 */}
        <div style={{ marginTop: 28 }}>
          <div style={{ fontSize: 12, fontWeight: 700, color: T2.textMute, letterSpacing: 0.5, textTransform: 'uppercase', marginBottom: 12 }}>
            좋아하는 음식 <span style={{ color: T2.textMute, fontWeight: 500 }}>· 최대 3개</span>
          </div>
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
            {[
            { l: '한식', on: true }, { l: '일식', on: true }, { l: '양식', on: false },
            { l: '중식', on: false }, { l: '면 요리', on: true }, { l: '매운맛', on: false },
            { l: '디저트', on: false }].
            map((f, i) =>
            <div key={i} style={{
              padding: '9px 14px', borderRadius: 999, fontSize: 13, fontWeight: 600, letterSpacing: -0.2,
              background: f.on ? T2.brand : '#fff', color: f.on ? '#fff' : T2.text,
              border: `1px solid ${f.on ? T2.brand : T2.border}`
            }}>{f.l}</div>
            )}
          </div>
        </div>

        {/* 같이 먹을 때 */}
        <div style={{ marginTop: 28 }}>
          <div style={{ fontSize: 12, fontWeight: 700, color: T2.textMute, letterSpacing: 0.5, textTransform: 'uppercase', marginBottom: 12 }}>
            같이 먹을 때
          </div>
          {[
          { l: '도란도란 대화하며', s: '가볍게 이야기 나누는 게 좋아요', on: true },
          { l: '조용히 각자', s: '편하게, 말 없이 먹어도 좋아요', on: false }].
          map((r, i) =>
          <div key={i} style={{
            display: 'flex', alignItems: 'center', gap: 12, padding: '14px 16px',
            borderRadius: 12, marginBottom: 8,
            background: r.on ? T2.text : '#fff',
            border: `1px solid ${r.on ? T2.text : T2.border}`
          }}>
              <div style={{
              width: 18, height: 18, borderRadius: '50%',
              border: `2px solid ${r.on ? T2.brand : T2.borderStrong}`,
              background: r.on ? T2.brand : 'transparent', flexShrink: 0,
              boxShadow: r.on ? `inset 0 0 0 3px ${T2.text}` : 'none'
            }} />
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 15, fontWeight: 700, color: r.on ? '#fff' : T2.text, letterSpacing: -0.3 }}>{r.l}</div>
                <div style={{ fontSize: 12, color: r.on ? 'rgba(255,255,255,0.6)' : T2.textMute, marginTop: 1 }}>{r.s}</div>
              </div>
            </div>
          )}
        </div>

        {/* 동네 */}
        <div style={{ marginTop: 28 }}>
          <div style={{ fontSize: 12, fontWeight: 700, color: T2.textMute, letterSpacing: 0.5, textTransform: 'uppercase', marginBottom: 10 }}>
            우리 동네
          </div>
          <div style={{
            display: 'flex', alignItems: 'center', gap: 12, padding: '14px 16px',
            background: '#fff', borderRadius: 12, border: `1px solid ${T2.border}`
          }}>
            <div style={{ width: 8, height: 8, borderRadius: '50%', background: T2.brand, flexShrink: 0 }} />
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 15, fontWeight: 700, color: T2.text, letterSpacing: -0.3 }}>마포구 연남동</div>
              <div style={{ fontSize: 11, color: T2.textMute, marginTop: 2 }}>현재 위치 기반 · 식당 142곳</div>
            </div>
            <span style={{ fontSize: 13, fontWeight: 700, color: T2.text }}>변경</span>
          </div>
        </div>

        {/* 인증 배지 — 신뢰 정보 */}
        <div style={{ marginTop: 16, marginBottom: 8, padding: '14px 16px', borderRadius: 12, background: 'rgba(255,90,31,0.06)', border: `1px solid rgba(255,90,31,0.15)`, display: 'flex', alignItems: 'center', gap: 12 }}>
          <div style={{
            width: 32, height: 32, borderRadius: '50%', background: T2.brand, flexShrink: 0,
            display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff', fontSize: 15, fontWeight: 800
          }}>✓</div>
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 13, fontWeight: 700, color: T2.text, letterSpacing: -0.3 }}>휴대폰 인증 완료</div>
            <div style={{ fontSize: 12, color: T2.textSub, marginTop: 1, letterSpacing: -0.2 }}>상대에게 ‘인증된 사용자’로 표시돼요</div>
          </div>
        </div>
      </div>

      {/* CTA */}
      <div style={{ position: 'absolute', bottom: 0, left: 0, right: 0, padding: '12px 24px 44px', background: T2.bg }}>
        <div style={{
          padding: '16px', borderRadius: 12, background: T2.brand, color: '#fff',
          fontSize: 15, fontWeight: 700, textAlign: 'center', letterSpacing: -0.3
        }}>시작하기</div>
      </div>
    </PhoneShell>);

}

Object.assign(window, { ProfileSetup });
