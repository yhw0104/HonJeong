// MateProfile.jsx — 메이트 프로필
// (구 Min_MateProfile)

// ───────────────────────────────────────────────────────────
// 더보기 7: 메이트 프로필 (다른 사람)
// ───────────────────────────────────────────────────────────
function MateProfile() {
  const foods = ['한식', '면 요리', '디저트'];
  const stats = [
    { n: '32', l: '혼밥' },
    { n: '2', l: '함께 먹음' },
    { n: '9', l: '뱃지' },
  ];
  // 메이트가 공개로 설정해둔 즐겨찾기 그룹만 노출
  const publicGroups = [
    { name: '혼밥 입문 코스', emo: '🍲', count: 5, note: '부담 없는 첫 혼밥' },
    { name: '연남 국수 지도', emo: '🍜', count: 3, note: '면 요리 모음' },
  ];

  return (
    <PhoneShell bg={T2.bg}>
      {/* 헤더 — 뒤로 + 더보기 */}
      <div style={{
        position: 'absolute', top: 56, left: 0, right: 0, height: 52, zIndex: 10,
        display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 12px',
      }}>
        <div style={{ width: 40, height: 40, borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }}>
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none"><path d="M15 6l-6 6 6 6" stroke={T2.text} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/></svg>
        </div>
        <div style={{ width: 40, height: 40, borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }}>
          <svg width="20" height="20" viewBox="0 0 24 24" fill={T2.text}><circle cx="5" cy="12" r="1.6"/><circle cx="12" cy="12" r="1.6"/><circle cx="19" cy="12" r="1.6"/></svg>
        </div>
      </div>

      <div style={{ position: 'absolute', top: 108, left: 0, right: 0, bottom: 96, overflow: 'auto', padding: '8px 20px 24px' }}>
        {/* 프로필 헤더 */}
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center', paddingTop: 8 }}>
          <div style={{ position: 'relative' }}>
            <div style={{
              width: 84, height: 84, borderRadius: '50%',
              background: T2.bg, border: `1px solid ${T2.border}`,
              display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 40,
            }}>🍙</div>
            {/* 혼밥 중 상태 점 */}
            <div style={{
              position: 'absolute', right: 4, bottom: 4, width: 20, height: 20, borderRadius: '50%',
              background: '#22A65A', border: '3px solid ' + T2.bg,
            }} />
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 14 }}>
            <span style={{ fontSize: 22, fontWeight: 800, color: T2.text, letterSpacing: -0.6 }}>점심혼밥러</span>
            <span style={{ fontSize: 10, fontWeight: 700, color: T2.brand, background: T2.brandSoft, padding: '2px 6px', borderRadius: 5 }}>같이 2회</span>
          </div>
          {/* 지금 혼밥 중 */}
          <div style={{
            display: 'inline-flex', alignItems: 'center', gap: 6, marginTop: 10,
            padding: '6px 12px', borderRadius: 999, background: 'rgba(34,166,90,0.1)',
          }}>
            <span style={{ width: 7, height: 7, borderRadius: '50%', background: '#22A65A' }} />
            <span style={{ fontSize: 12, fontWeight: 700, color: '#1B8049', letterSpacing: -0.2 }}>지금 혼밥 중</span>
            <span style={{ fontSize: 12, color: T2.textSub, letterSpacing: -0.2 }}>· 큰순두부 연남점</span>
          </div>
          <div style={{ fontSize: 13, color: T2.textMute, marginTop: 10, letterSpacing: -0.2 }}>연남동 · 혼밥 1년차</div>
          <div style={{ fontSize: 14, color: T2.textSub, marginTop: 14, lineHeight: 1.6, letterSpacing: -0.3, maxWidth: 280 }}>
            "점심은 거의 혼밥! 순두부랑 국수 좋아해요.<br/>편하게 같이 드실 분 환영이에요."
          </div>
        </div>

        {/* 통계 */}
        <div style={{
          display: 'flex', marginTop: 24, padding: '18px 0', background: '#fff',
          borderRadius: 18, border: `1px solid ${T2.border}`,
        }}>
          {stats.map((s, i) => (
            <div key={i} style={{ flex: 1, textAlign: 'center', borderLeft: i ? `1px solid ${T2.border}` : 'none' }}>
              <div style={{ fontSize: 22, fontWeight: 800, color: i === 1 ? T2.brand : T2.text, letterSpacing: -0.6, fontFeatureSettings: '"tnum"' }}>{s.n}</div>
              <div style={{ fontSize: 11, color: T2.textMute, marginTop: 4 }}>{s.l}</div>
            </div>
          ))}
        </div>

        {/* 좋아하는 음식 */}
        <div style={{ marginTop: 28 }}>
          <div style={{ fontSize: 11, fontWeight: 700, color: T2.textMute, letterSpacing: 0.6, textTransform: 'uppercase', marginBottom: 12 }}>좋아하는 음식</div>
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
          <div style={{ fontSize: 11, fontWeight: 700, color: T2.textMute, letterSpacing: 0.6, textTransform: 'uppercase', marginBottom: 12 }}>같이 먹을 때</div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, padding: 16, borderRadius: 14, background: T2.text }}>
            <div style={{ fontSize: 22, flexShrink: 0 }}>💬</div>
            <div>
              <div style={{ fontSize: 15, fontWeight: 700, color: '#fff', letterSpacing: -0.3 }}>도란도란 대화하며</div>
              <div style={{ fontSize: 12, color: 'rgba(255,255,255,0.6)', marginTop: 2 }}>가볍게 이야기 나누는 게 좋아요</div>
            </div>
          </div>
        </div>

        {/* 공개 즐겨찾기 — 메이트가 공개한 그룹만 */}
        <div style={{ marginTop: 28 }}>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: 6, marginBottom: 12 }}>
            <span style={{ fontSize: 11, fontWeight: 700, color: T2.textMute, letterSpacing: 0.6, textTransform: 'uppercase' }}>공개 즐겨찾기</span>
            <span style={{ fontSize: 11, fontWeight: 700, color: T2.brand, fontFeatureSettings: '"tnum"' }}>{publicGroups.length}</span>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {publicGroups.map((g, i) => (
              <div key={i} style={{
                display: 'flex', alignItems: 'center', gap: 13, padding: 14,
                background: '#fff', borderRadius: 16, border: `1px solid ${T2.border}`, cursor: 'pointer',
              }}>
                <div style={{
                  width: 50, height: 50, borderRadius: 12, flexShrink: 0,
                  background: T2.bg, border: `1px solid ${T2.border}`,
                  display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 22,
                }}>{g.emo}</div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                    <span style={{ fontSize: 15, fontWeight: 800, color: T2.text, letterSpacing: -0.3, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{g.name}</span>
                    <span style={{
                      display: 'flex', alignItems: 'center', gap: 3, flexShrink: 0,
                      fontSize: 10, fontWeight: 700, color: T2.textSub, background: T2.bg,
                      border: `1px solid ${T2.border}`, padding: '2px 6px', borderRadius: 5,
                    }}>
                      <svg width="9" height="9" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="3.5" stroke={T2.textSub} strokeWidth="2"/><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7-10-7-10-7z" stroke={T2.textSub} strokeWidth="2"/></svg>
                      공개
                    </span>
                  </div>
                  <div style={{ fontSize: 12, color: T2.textSub, marginTop: 4, display: 'flex', gap: 7, alignItems: 'center', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    <span style={{ fontWeight: 700, color: T2.text, fontFeatureSettings: '"tnum"', flexShrink: 0 }}>{g.count}곳</span>
                    <span style={{ color: T2.textMute, flexShrink: 0 }}>·</span>
                    <span style={{ overflow: 'hidden', textOverflow: 'ellipsis' }}>{g.note}</span>
                  </div>
                </div>
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" style={{ flexShrink: 0 }}><path d="M9 6l6 6-6 6" stroke={T2.textMute} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/></svg>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* 하단 고정 CTA — 팔로우 + 같이 먹기 신청 */}
      <div style={{
        position: 'absolute', left: 0, right: 0, bottom: 0, zIndex: 20,
        padding: '12px 16px 40px', background: '#fff',
        borderTop: `1px solid ${T2.border}`, display: 'flex', gap: 10,
      }}>
        <div style={{
          padding: '16px 18px', borderRadius: 12, background: T2.bg, color: T2.textSub,
          fontSize: 14, fontWeight: 700, letterSpacing: -0.3, display: 'flex', alignItems: 'center', gap: 6,
        }}>
          <span style={{ color: T2.brand, fontSize: 16 }}>✓</span> 메이트
        </div>
        <div style={{
          flex: 1, padding: '16px', borderRadius: 12, background: T2.brand, color: '#fff',
          fontSize: 15, fontWeight: 700, letterSpacing: -0.3, textAlign: 'center',
        }}>같이 먹기 신청</div>
      </div>
    </PhoneShell>
  );
}

Object.assign(window, { MateProfile });
