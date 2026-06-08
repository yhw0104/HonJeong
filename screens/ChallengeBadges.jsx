// ChallengeBadges.jsx — 혼밥 챌린지 · 뱃지
// (구 Min_Badges)

// ───────────────────────────────────────────────────────────
// 더보기 2: 혼밥 챌린지 · 뱃지
// ───────────────────────────────────────────────────────────
function ChallengeBadges() {
  const badges = [
    { e: '🌱', n: '첫 혼밥', got: true },
    { e: '🍚', n: '혼밥 10회', got: true },
    { e: '🔥', n: '3일 연속', got: true },
    { e: '🍜', n: '한식 마스터', got: true },
    { e: '🤝', n: '첫 같이 먹기', got: true },
    { e: '📷', n: '일기 10편', got: true },
    { e: '🌙', n: '혼밥 디너', got: true },
    { e: '🏆', n: '혼밥 50회', got: false },
    { e: '🗺️', n: '동네 정복', got: false },
    { e: '⭐', n: '리뷰 30개', got: false },
    { e: '🎂', n: '생일 혼밥', got: false },
    { e: '🥇', n: '레벨 5', got: false },
  ];

  return (
    <PhoneShell bg={T2.bg}>
      <MoreHeader title="혼밥 챌린지 · 뱃지" />

      <div style={{ position: 'absolute', top: 108, left: 0, right: 0, bottom: 0, overflow: 'auto', padding: '8px 20px 40px' }}>
        {/* 진행 중 챌린지 */}
        <div style={{
          padding: 20, borderRadius: 18, background: T2.text, color: '#fff', marginTop: 4,
        }}>
          <div style={{ fontSize: 11, fontWeight: 700, color: 'rgba(255,255,255,0.6)', letterSpacing: 0.6, textTransform: 'uppercase' }}>이번 주 챌린지</div>
          <div style={{ fontSize: 19, fontWeight: 800, letterSpacing: -0.5, marginTop: 8 }}>새로운 동네에서 혼밥하기</div>
          <div style={{ marginTop: 16 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, fontWeight: 600, marginBottom: 7 }}>
              <span style={{ color: 'rgba(255,255,255,0.7)' }}>진행률</span>
              <span><b style={{ color: T2.brand }}>2</b> / 3 곳</span>
            </div>
            <div style={{ height: 7, borderRadius: 4, background: 'rgba(255,255,255,0.15)', overflow: 'hidden' }}>
              <div style={{ width: '66%', height: '100%', background: T2.brand, borderRadius: 4 }} />
            </div>
          </div>
        </div>

        {/* 뱃지 진행 요약 */}
        <div style={{ display: 'flex', alignItems: 'baseline', gap: 8, marginTop: 28, marginBottom: 14 }}>
          <span style={{ fontSize: 15, fontWeight: 800, color: T2.text, letterSpacing: -0.4 }}>내 뱃지</span>
          <span style={{ fontSize: 13, fontWeight: 700, color: T2.textMute }}>7 / 12 획득</span>
        </div>

        {/* 뱃지 그리드 */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12 }}>
          {badges.map((b, i) => (
            <div key={i} style={{
              padding: '18px 8px 14px', borderRadius: 16, textAlign: 'center',
              background: b.got ? '#fff' : T2.bg,
              border: `1px solid ${b.got ? T2.border : 'transparent'}`,
              opacity: b.got ? 1 : 0.5,
            }}>
              <div style={{
                width: 52, height: 52, borderRadius: '50%', margin: '0 auto',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                fontSize: 26, background: b.got ? T2.brandSoft : 'rgba(0,0,0,0.04)',
                filter: b.got ? 'none' : 'grayscale(1)',
              }}>{b.got ? b.e : '🔒'}</div>
              <div style={{ fontSize: 12, fontWeight: 700, color: b.got ? T2.text : T2.textMute, marginTop: 10, letterSpacing: -0.2 }}>{b.n}</div>
            </div>
          ))}
        </div>
      </div>
    </PhoneShell>
  );
}

Object.assign(window, { ChallengeBadges });
