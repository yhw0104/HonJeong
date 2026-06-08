// ProfileEdit.jsx — 프로필 편집
// (구 Min_ProfileEdit)

// ───────────────────────────────────────────────────────────
// 더보기 5: 프로필 편집
// ───────────────────────────────────────────────────────────
function ProfileEdit() {
  const FieldLabel = ({ children }) => (
    <div style={{ fontSize: 11, fontWeight: 700, color: T2.textMute, letterSpacing: 0.6, textTransform: 'uppercase', marginBottom: 10 }}>{children}</div>
  );

  return (
    <PhoneShell bg={T2.bg}>
      {/* 헤더 — 취소 / 제목 / 저장 */}
      <div style={{
        position: 'absolute', top: 56, left: 0, right: 0, height: 52, zIndex: 10,
        display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 20px',
      }}>
        <div style={{ fontSize: 14, fontWeight: 600, color: T2.textSub, letterSpacing: -0.2, cursor: 'pointer' }}>취소</div>
        <div style={{ fontSize: 15, fontWeight: 800, color: T2.text, letterSpacing: -0.3 }}>프로필 편집</div>
        <div style={{ fontSize: 14, fontWeight: 700, color: T2.brand, letterSpacing: -0.2, cursor: 'pointer' }}>저장</div>
      </div>

      <div style={{ position: 'absolute', top: 108, left: 0, right: 0, bottom: 0, overflow: 'auto', padding: '12px 20px 40px' }}>
        {/* 사진 변경 */}
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', paddingBottom: 8 }}>
          <div style={{ position: 'relative' }}>
            <Avatar name="혼" bg={T2.text} size={84} />
            <div style={{
              position: 'absolute', right: -2, bottom: -2,
              width: 30, height: 30, borderRadius: '50%', background: T2.brand,
              border: '3px solid ' + T2.bg, display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}>
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none">
                <path d="M4 8a2 2 0 0 1 2-2h2l1.5-2h5L18 6h0a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V8z" stroke="#fff" strokeWidth="1.7" strokeLinejoin="round"/>
                <circle cx="12" cy="12.5" r="3" stroke="#fff" strokeWidth="1.7"/>
              </svg>
            </div>
          </div>
          <div style={{ fontSize: 13, fontWeight: 700, color: T2.brand, marginTop: 12, letterSpacing: -0.2, cursor: 'pointer' }}>사진 변경</div>
        </div>

        {/* 닉네임 */}
        <div style={{ marginTop: 20 }}>
          <FieldLabel>닉네임</FieldLabel>
          <div style={{
            display: 'flex', alignItems: 'center', padding: '0 16px', height: 52,
            background: '#fff', borderRadius: 12, border: `1px solid ${T2.border}`,
          }}>
            <span style={{ flex: 1, fontSize: 15, fontWeight: 600, color: T2.text, letterSpacing: -0.3 }}>조용한혼밥러</span>
            <span style={{ fontSize: 12, color: T2.textMute, fontFeatureSettings: '"tnum"' }}>6 / 12</span>
          </div>
        </div>

        {/* 한 줄 소개 */}
        <div style={{ marginTop: 24 }}>
          <FieldLabel>한 줄 소개</FieldLabel>
          <div style={{
            padding: 16, minHeight: 76, background: '#fff', borderRadius: 12,
            border: `1px solid ${T2.border}`, fontSize: 14, color: T2.text, lineHeight: 1.6, letterSpacing: -0.3,
          }}>
            혼자 먹는 시간이 좋아졌어요. 가끔은 같이 먹는 것도요 :)
          </div>
        </div>

        {/* 내 동네 */}
        <div style={{ marginTop: 24 }}>
          <FieldLabel>내 동네</FieldLabel>
          <div style={{
            display: 'flex', alignItems: 'center', gap: 8, padding: '0 16px', height: 52,
            background: '#fff', borderRadius: 12, border: `1px solid ${T2.border}`,
          }}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M12 2C8.1 2 5 5.1 5 9c0 5.2 7 13 7 13s7-7.8 7-13c0-3.9-3.1-7-7-7z" stroke={T2.textMute} strokeWidth="1.6" strokeLinejoin="round"/>
              <circle cx="12" cy="9" r="2.4" stroke={T2.textMute} strokeWidth="1.6"/>
            </svg>
            <span style={{ flex: 1, fontSize: 15, fontWeight: 600, color: T2.text, letterSpacing: -0.3 }}>연남동</span>
            <span style={{ fontSize: 13, fontWeight: 700, color: T2.brand }}>변경</span>
          </div>
        </div>

        {/* 좋아하는 음식 */}
        <div style={{ marginTop: 24 }}>
          <FieldLabel>좋아하는 음식 · 최대 3개</FieldLabel>
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
            {[
              { l: '한식', on: true }, { l: '일식', on: true }, { l: '양식', on: false },
              { l: '중식', on: false }, { l: '면 요리', on: true }, { l: '매운맛', on: false },
              { l: '디저트', on: false },
            ].map((f, i) => (
              <div key={i} style={{
                padding: '9px 14px', borderRadius: 999, fontSize: 13, fontWeight: 600, letterSpacing: -0.2,
                background: f.on ? T2.brand : '#fff', color: f.on ? '#fff' : T2.text,
                border: `1px solid ${f.on ? T2.brand : T2.border}`, cursor: 'pointer',
              }}>{f.l}</div>
            ))}
          </div>
        </div>

        {/* 같이 먹을 때 */}
        <div style={{ marginTop: 24 }}>
          <FieldLabel>같이 먹을 때</FieldLabel>
          {[
            { l: '도란도란 대화하며', s: '가볍게 이야기 나누는 게 좋아요', on: true },
            { l: '조용히 각자', s: '편하게, 말 없이 먹어도 좋아요', on: false },
          ].map((r, i) => (
            <div key={i} style={{
              display: 'flex', alignItems: 'center', gap: 12, padding: '14px 16px',
              borderRadius: 12, marginBottom: 8, cursor: 'pointer',
              background: r.on ? T2.text : '#fff',
              border: `1px solid ${r.on ? T2.text : T2.border}`,
            }}>
              <div style={{
                width: 18, height: 18, borderRadius: '50%',
                border: `2px solid ${r.on ? T2.brand : T2.borderStrong}`,
                background: r.on ? T2.brand : 'transparent', flexShrink: 0,
                boxShadow: r.on ? `inset 0 0 0 3px ${T2.text}` : 'none',
              }} />
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 15, fontWeight: 700, color: r.on ? '#fff' : T2.text, letterSpacing: -0.3 }}>{r.l}</div>
                <div style={{ fontSize: 12, color: r.on ? 'rgba(255,255,255,0.6)' : T2.textMute, marginTop: 1 }}>{r.s}</div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </PhoneShell>
  );
}

Object.assign(window, { ProfileEdit });
