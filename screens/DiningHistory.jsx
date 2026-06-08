// DiningHistory.jsx — 내 혼밥 기록
// (구 Min_Log)

// ───────────────────────────────────────────────────────────
// 더보기 1: 내 혼밥 기록
// ───────────────────────────────────────────────────────────
function DiningHistory() {
  const months = [
    {
      m: '2026년 5월',
      entries: [
        { d: '22', day: 'FRI', place: '큰순두부 연남점', note: '벽 보고 앉아서 마음 편히 먹었다.', taste: '5.0', honbab: '5.0', photo: true },
        { d: '20', day: 'WED', place: '연남 김밥', empty: true },
        { d: '18', day: 'MON', place: '혼밥의자', note: '바테이블 끝자리. 책 읽으며 30분.', taste: '4.5', honbab: '4.5', photo: true },
        { d: '11', day: 'MON', place: '옥상국밥', note: '점심 빠르게. 1인석 바로 앉음.', taste: '4.0', honbab: '4.0', photo: false },
      ],
    },
    {
      m: '2026년 4월',
      entries: [
        { d: '29', day: 'TUE', place: '연남 파스타바', note: '큰맘 먹고 양식집 혼밥 첫 도전!', taste: '4.5', honbab: '4.0', photo: true },
        { d: '25', day: 'FRI', place: '망원 우동집', empty: true },
        { d: '20', day: 'SUN', place: '큰순두부 연남점', note: '주말 브런치. 한산해서 좋았음.', taste: '5.0', honbab: '5.0', photo: false },
      ],
    },
  ];

  return (
    <PhoneShell bg={T2.bg}>
      <MoreHeader title="내 혼밥 기록" />

      <div style={{ position: 'absolute', top: 108, left: 0, right: 0, bottom: 0, overflow: 'auto', padding: '8px 20px 40px' }}>
        {/* 요약 통계 */}
        <div style={{
          display: 'flex', padding: '18px 0 22px', gap: 0,
          borderBottom: `1px solid ${T2.border}`,
        }}>
          {[
            { n: '32', l: '총 혼밥' },
            { n: '28', l: '일기' },
            { n: '12', l: '식당' },
            { n: '5', l: '이번달' },
          ].map((s, i) => (
            <div key={i} style={{ flex: 1, textAlign: 'center', borderLeft: i ? `1px solid ${T2.border}` : 'none' }}>
              <div style={{ fontSize: 24, fontWeight: 800, color: i === 3 ? T2.brand : T2.text, letterSpacing: -0.8, fontFeatureSettings: '"tnum"' }}>{s.n}</div>
              <div style={{ fontSize: 11, color: T2.textMute, marginTop: 3 }}>{s.l}</div>
            </div>
          ))}
        </div>

        {/* 월별 기록 */}
        {months.map((mo, mi) => (
          <div key={mi} style={{ marginTop: 24 }}>
            <div style={{ fontSize: 13, fontWeight: 800, color: T2.text, letterSpacing: -0.3, marginBottom: 12 }}>{mo.m}</div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
              {mo.entries.map((e, ei) => (
                e.empty ? (
                  /* 일기 없는 방문 — 흐린 미니 카드 */
                  <div key={ei} style={{
                    display: 'flex', alignItems: 'center', gap: 14, padding: '12px 14px',
                    background: 'transparent', borderRadius: 16,
                    border: `1px dashed ${T2.borderStrong}`,
                  }}>
                    <div style={{ width: 40, flexShrink: 0, textAlign: 'center', opacity: 0.55 }}>
                      <div style={{ fontSize: 20, fontWeight: 800, color: T2.textSub, letterSpacing: -0.5, lineHeight: 1, fontFeatureSettings: '"tnum"' }}>{e.d}</div>
                      <div style={{ fontSize: 10, fontWeight: 700, color: T2.textMute, marginTop: 3, letterSpacing: 0.5 }}>{e.day}</div>
                    </div>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ fontSize: 14, fontWeight: 700, color: T2.textSub, letterSpacing: -0.3 }}>{e.place}</div>
                      <div style={{ fontSize: 12, color: T2.textMute, marginTop: 4, letterSpacing: -0.2 }}>혼밥 기록 · 일기 없음</div>
                    </div>
                    <div style={{
                      flexShrink: 0, display: 'flex', alignItems: 'center', gap: 5, cursor: 'pointer',
                      padding: '8px 12px', borderRadius: 9, background: T2.brandSoft, color: T2.brand,
                      fontSize: 12, fontWeight: 700, letterSpacing: -0.2,
                    }}>
                      <svg width="13" height="13" viewBox="0 0 24 24" fill="none">
                        <path d="M4 20h4l10-10-4-4L4 16v4z" stroke={T2.brand} strokeWidth="1.8" strokeLinejoin="round"/>
                        <path d="M13.5 6.5l4 4" stroke={T2.brand} strokeWidth="1.8" strokeLinecap="round"/>
                      </svg>
                      일기 쓰기
                    </div>
                  </div>
                ) : (
                <div key={ei} style={{
                  display: 'flex', gap: 14, padding: 14, background: '#fff',
                  borderRadius: 16, border: `1px solid ${T2.border}`,
                }}>
                  {/* 날짜 */}
                  <div style={{ width: 40, flexShrink: 0, textAlign: 'center' }}>
                    <div style={{ fontSize: 20, fontWeight: 800, color: T2.text, letterSpacing: -0.5, lineHeight: 1, fontFeatureSettings: '"tnum"' }}>{e.d}</div>
                    <div style={{ fontSize: 10, fontWeight: 700, color: T2.textMute, marginTop: 3, letterSpacing: 0.5 }}>{e.day}</div>
                  </div>
                  {/* 본문 */}
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: 14, fontWeight: 700, color: T2.text, letterSpacing: -0.3 }}>{e.place}</div>
                    <div style={{ fontSize: 12, color: T2.textSub, lineHeight: 1.5, marginTop: 5, letterSpacing: -0.2 }}>{e.note}</div>
                    <div style={{ display: 'flex', gap: 6, marginTop: 10 }}>
                      <span style={{ fontSize: 11, fontWeight: 700, color: T2.textSub, background: T2.bg, border: `1px solid ${T2.border}`, padding: '3px 7px', borderRadius: 6 }}>맛 ★ {e.taste}</span>
                      <span style={{ fontSize: 11, fontWeight: 700, color: T2.brand, background: T2.brandSoft, padding: '3px 7px', borderRadius: 6 }}>혼밥 ★ {e.honbab}</span>
                    </div>
                  </div>
                  {/* 썸네일 */}
                  {e.photo && (
                    <ImagePlaceholder w={56} h={56} radius={12} bg="#EEE9DF" stripe="#E0D9C7" color="#A39B85" />
                  )}
                </div>
                )
              ))}
            </div>
          </div>
        ))}
      </div>
    </PhoneShell>
  );
}

Object.assign(window, { DiningHistory });
