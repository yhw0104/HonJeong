// MyProfile.jsx — 내 프로필
// (구 Min_Profile)

// ───────────────────────────────────────────────────────────
// 더보기 4: 내 프로필 (프로필 카드 탭 시)
// ───────────────────────────────────────────────────────────
function MyProfile() {
  const foods = ['한식', '일식', '면 요리'];
  const stats = [
    { n: '32', l: '혼밥' },
    { n: '7', l: '메이트' },
    { n: '7', l: '뱃지' },
  ];

  return (
    <PhoneShell bg={T2.bg}>
      {/* 헤더 — 뒤로 + 편집 */}
      <div style={{
        position: 'absolute', top: 56, left: 0, right: 0, height: 52, zIndex: 10,
        display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 12px',
      }}>
        <div style={{ width: 40, height: 40, borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }}>
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none"><path d="M15 6l-6 6 6 6" stroke={T2.text} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/></svg>
        </div>
        <div style={{ fontSize: 14, fontWeight: 700, color: T2.brand, letterSpacing: -0.2, padding: '0 8px', cursor: 'pointer' }}>편집</div>
      </div>

      <div style={{ position: 'absolute', top: 108, left: 0, right: 0, bottom: 0, overflow: 'auto', padding: '8px 20px 40px' }}>
        {/* 프로필 헤더 */}
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center', paddingTop: 8 }}>
          <Avatar name="혼" bg={T2.text} size={84} />
          <div style={{ fontSize: 22, fontWeight: 800, color: T2.text, letterSpacing: -0.6, marginTop: 14 }}>조용한혼밥러</div>
          <div style={{ fontSize: 13, color: T2.textMute, marginTop: 5, letterSpacing: -0.2 }}>연남동 · 혼밥 입문 6개월차</div>
          <div style={{ fontSize: 14, color: T2.textSub, marginTop: 14, lineHeight: 1.6, letterSpacing: -0.3, maxWidth: 280 }}>
            "혼자 먹는 시간이 좋아졌어요.<br/>가끔은 같이 먹는 것도요 :)"
          </div>
        </div>

        {/* 통계 */}
        <div style={{
          display: 'flex', marginTop: 24, padding: '18px 0', background: '#fff',
          borderRadius: 18, border: `1px solid ${T2.border}`,
        }}>
          {stats.map((s, i) => (
            <div key={i} style={{ flex: 1, textAlign: 'center', borderLeft: i ? `1px solid ${T2.border}` : 'none' }}>
              <div style={{ fontSize: 22, fontWeight: 800, color: T2.text, letterSpacing: -0.6, fontFeatureSettings: '"tnum"' }}>{s.n}</div>
              <div style={{ fontSize: 11, color: T2.textMute, marginTop: 4 }}>{s.l}</div>
            </div>
          ))}
        </div>

        {/* 좋아하는 음식 */}
        <div style={{ marginTop: 28 }}>
          <div style={{ fontSize: 11, fontWeight: 700, color: T2.textMute, letterSpacing: 0.6, textTransform: 'uppercase', marginBottom: 12 }}>
            좋아하는 음식
          </div>
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
            {foods.map((f, i) => (
              <div key={i} style={{
                padding: '9px 14px', borderRadius: 999, fontSize: 13, fontWeight: 600, letterSpacing: -0.2,
                background: '#fff', color: T2.text, border: `1px solid ${T2.borderStrong}`,
              }}>{f}</div>
            ))}
          </div>
        </div>

        {/* 같이 먹을 때 */}
        <div style={{ marginTop: 28 }}>
          <div style={{ fontSize: 11, fontWeight: 700, color: T2.textMute, letterSpacing: 0.6, textTransform: 'uppercase', marginBottom: 12 }}>
            같이 먹을 때
          </div>
          <div style={{
            display: 'flex', alignItems: 'center', gap: 12, padding: 16,
            borderRadius: 14, background: T2.text,
          }}>
            <div style={{ fontSize: 22, flexShrink: 0 }}>💬</div>
            <div>
              <div style={{ fontSize: 15, fontWeight: 700, color: '#fff', letterSpacing: -0.3 }}>도란도란 대화하며</div>
              <div style={{ fontSize: 12, color: 'rgba(255,255,255,0.6)', marginTop: 2 }}>가볍게 이야기 나누는 게 좋아요</div>
            </div>
          </div>
        </div>

        {/* 최근 획득 뱃지 */}
        <div style={{ marginTop: 28 }}>
          <div style={{ display: 'flex', alignItems: 'center', marginBottom: 12 }}>
            <span style={{ flex: 1, fontSize: 11, fontWeight: 700, color: T2.textMute, letterSpacing: 0.6, textTransform: 'uppercase' }}>최근 획득 뱃지</span>
            <span style={{ fontSize: 12, fontWeight: 700, color: T2.brand, cursor: 'pointer' }}>전체보기</span>
          </div>
          <div style={{ display: 'flex', gap: 10 }}>
            {['🌱', '🍚', '🔥', '🤝'].map((e, i) => (
              <div key={i} style={{
                flex: 1, aspectRatio: '1', borderRadius: 14, background: '#fff',
                border: `1px solid ${T2.border}`,
                display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 24,
              }}>{e}</div>
            ))}
          </div>
        </div>
      </div>
    </PhoneShell>
  );
}

Object.assign(window, { MyProfile });
