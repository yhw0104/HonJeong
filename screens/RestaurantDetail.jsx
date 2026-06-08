// RestaurantDetail.jsx — 식당 상세
// (구 Min_Detail)

// ───────────────────────────────────────────────────────────
// 화면 2: 식당 상세
// ───────────────────────────────────────────────────────────
function RestaurantDetail() {
  const [stab, setStab] = React.useState('home');
  const [honbabOn, setHonbabOn] = React.useState(false);
  return (
    <PhoneShell bg={T2.bg}>
      {/* 혼밥 중 상태 바 — 토글 ON 동안 표시 */}
      {honbabOn && <HonbabStatusBar place="큰순두부 연남점" onEnd={() => setHonbabOn(false)} />}

      {/* 헤더 — 풀스크린 위에 떠있는 버튼만 */}
      <div style={{
        position: 'absolute', top: honbabOn ? 116 : 60, left: 16, right: 16, display: 'flex',
        justifyContent: 'space-between', zIndex: 20, transition: 'top 0.2s',
      }}>
        <div style={{
          width: 38, height: 38, borderRadius: '50%', background: 'rgba(255,255,255,0.95)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          boxShadow: '0 2px 6px rgba(0,0,0,0.1)', fontSize: 18,
        }}>←</div>
        <div style={{ display: 'flex', gap: 8 }}>
          {['↗︎', '♡'].map((s, i) => (
            <div key={i} style={{
              width: 38, height: 38, borderRadius: '50%', background: 'rgba(255,255,255,0.95)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              boxShadow: '0 2px 6px rgba(0,0,0,0.1)', fontSize: 14,
            }}>{s}</div>
          ))}
        </div>
      </div>

      {/* 콘텐츠 */}
      <div style={{ position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, overflow: 'auto', background: T2.bg }}>
        {/* 작은 히어로 — 4:3 절제된 사이즈 */}
        <div style={{ height: 320, background: '#EEE9DF', position: 'relative' }}>
          <ImagePlaceholder w="100%" h={320} radius={0} bg="#EEE9DF" stripe="#E0D9C7" color="#A39B85" tag="대표 사진 · 1/24" />
        </div>

        <div style={{ padding: '28px 20px 140px', background: T2.bg }}>
          {/* 카테고리 + 영업 — 2줄 */}
          <div style={{ marginBottom: 10 }}>
            <div style={{ fontSize: 11, fontWeight: 700, color: T2.textMute, letterSpacing: 0.6, textTransform: 'uppercase' }}>
              한식 · 순두부
            </div>
            <span style={{
              display: 'inline-block', marginTop: 8,
              padding: '3px 7px', background: T2.text, color: '#fff',
              borderRadius: 4, fontSize: 10, fontWeight: 700, letterSpacing: 0.2,
            }}>영업중 · 21시까지</span>
          </div>

          <h1 style={{
            fontSize: 26, fontWeight: 800, color: T2.text, letterSpacing: -0.8,
            margin: 0, lineHeight: 1.15,
          }}>큰순두부<br/>연남점</h1>

          {/* 주소 + 복사 */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginTop: 12 }}>
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" style={{ flexShrink: 0 }}>
              <path d="M12 2C8.1 2 5 5.1 5 9c0 5.2 7 13 7 13s7-7.8 7-13c0-3.9-3.1-7-7-7z" stroke={T2.textMute} strokeWidth="1.6" strokeLinejoin="round"/>
              <circle cx="12" cy="9" r="2.4" stroke={T2.textMute} strokeWidth="1.6"/>
            </svg>
            <span style={{ flex: 1, fontSize: 13, color: T2.textSub, letterSpacing: -0.3, lineHeight: 1.4 }}>
              서울 마포구 연남로 23길 14, 1층
            </span>
            <div
              onClick={(e) => {
                navigator.clipboard?.writeText('서울 마포구 연남로 23길 14, 1층');
                const el = e.currentTarget;
                const t = el.querySelector('span'); const orig = t.textContent;
                t.textContent = '복사됨'; el.style.color = T2.brand;
                setTimeout(() => { t.textContent = orig; el.style.color = T2.textSub; }, 1200);
              }}
              style={{
                display: 'flex', alignItems: 'center', gap: 4, flexShrink: 0, cursor: 'pointer',
                padding: '5px 9px', borderRadius: 8, border: `1px solid ${T2.border}`,
                color: T2.textSub, background: '#fff',
              }}>
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none">
                <rect x="8" y="8" width="11" height="13" rx="2" stroke="currentColor" strokeWidth="1.8"/>
                <path d="M16 5H7a2 2 0 0 0-2 2v10" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"/>
              </svg>
              <span style={{ fontSize: 12, fontWeight: 700, letterSpacing: -0.2 }}>복사</span>
            </div>
          </div>

          <div style={{
            display: 'flex', alignItems: 'baseline', gap: 14, marginTop: 14,
            paddingBottom: 18, borderBottom: `1px solid ${T2.border}`,
          }}>
            <div>
              <div style={{ fontSize: 24, fontWeight: 800, letterSpacing: -0.8, color: T2.text }}>4.3</div>
              <div style={{ fontSize: 11, color: T2.textMute, marginTop: 2 }}>리뷰 218</div>
            </div>
            <div style={{ width: 1, height: 32, background: T2.border }} />
            <div>
              <div style={{ fontSize: 24, fontWeight: 800, letterSpacing: -0.8, color: T2.brand }}>4.6</div>
              <div style={{ fontSize: 11, color: T2.textMute, marginTop: 2 }}>혼밥 친화도</div>
            </div>
          </div>

          {/* 가게 탭 — 홈 · 메뉴 · 리뷰 · 사진 */}
          <div style={{
            display: 'flex', justifyContent: 'space-between', marginTop: 4, marginBottom: 4,
            borderBottom: `1px solid ${T2.border}`,
          }}>
            {[
              { key: 'home', label: '홈' },
              { key: 'menu', label: '메뉴' },
              { key: 'review', label: '리뷰' },
              { key: 'photo', label: '사진' },
              { key: 'mate', label: '메이트' },
              { key: 'nearby', label: '주변' },
            ].map((t) => {
              const on = stab === t.key;
              return (
                <div key={t.key} onClick={() => setStab(t.key)} style={{
                  position: 'relative', flex: 1, paddingTop: 12, paddingBottom: 12, cursor: 'pointer',
                  fontSize: 15, fontWeight: on ? 800 : 600, textAlign: 'center',
                  color: on ? T2.text : T2.textMute, letterSpacing: -0.4,
                }}>
                  {t.label}
                  {on && <div style={{ position: 'absolute', left: 0, right: 0, bottom: -1, height: 2, background: T2.brand }} />}
                </div>
              );
            })}
          </div>

          {stab === 'home' && (
          <React.Fragment>
          {/* 혼밥 친화도 카드 */}
          <div style={{ marginTop: 24, padding: 20, borderRadius: 16, background: '#fff', border: `1px solid ${T2.border}` }}>
            <div style={{ display: 'flex', alignItems: 'flex-end', gap: 12 }}>
              <div>
                <div style={{ fontSize: 11, fontWeight: 700, color: T2.textMute, letterSpacing: 0.6, textTransform: 'uppercase' }}>혼밥 친화도</div>
                <div style={{ display: 'flex', alignItems: 'baseline', gap: 4, marginTop: 7 }}>
                  <span style={{ fontSize: 34, fontWeight: 800, color: T2.text, letterSpacing: -1.5, lineHeight: 1 }}>4.6</span>
                  <span style={{ fontSize: 14, fontWeight: 600, color: T2.textMute }}>/ 5</span>
                </div>
              </div>
              <div style={{ flex: 1 }} />
              <div style={{ textAlign: 'right' }}>
                <span style={{
                  display: 'inline-block', padding: '5px 10px', borderRadius: 999,
                  background: T2.brandSoft, color: T2.brand, fontSize: 12, fontWeight: 800, letterSpacing: -0.3,
                }}>혼밥하기 아주 좋아요</span>
                <div style={{ fontSize: 11, color: T2.textMute, marginTop: 7, letterSpacing: -0.2 }}>혼밥러 88명 평가</div>
              </div>
            </div>

            {/* 점수 바 */}
            <div style={{ display: 'flex', gap: 5, marginTop: 16 }}>
              {[0,1,2,3,4].map(i => {
                const fill = Math.max(0, Math.min(1, 4.6 - i));
                return (
                  <div key={i} style={{ flex: 1, height: 6, borderRadius: 3, background: T2.border, overflow: 'hidden' }}>
                    <div style={{ width: `${fill * 100}%`, height: '100%', background: T2.brand, borderRadius: 3 }} />
                  </div>
                );
              })}
            </div>

            <div style={{ height: 1, background: T2.border, margin: '18px 0 16px' }} />

            {/* 친화 요소 칩 */}
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 7 }}>
              {[
                { l: '1인석', on: true },
                { l: '바테이블', on: true },
                { l: '칸막이', on: true },
                { l: '눈치 없음', on: true },
                { l: '오래 OK', on: false },
              ].map((b, i) => (
                <div key={i} style={{
                  display: 'flex', alignItems: 'center', gap: 5,
                  padding: '8px 13px', borderRadius: 999,
                  fontSize: 13, fontWeight: 700, letterSpacing: -0.3,
                  background: b.on ? T2.brandSoft : T2.bg,
                  color: b.on ? T2.brand : T2.textMute,
                  border: `1px solid ${b.on ? 'rgba(255,90,31,0.2)' : T2.border}`,
                  opacity: b.on ? 1 : 0.7,
                }}>
                  {b.on ? (
                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none"><path d="M5 12.5l4.5 4.5L19 7" stroke={T2.brand} strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round"/></svg>
                  ) : (
                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none"><path d="M6 12h12" stroke={T2.textMute} strokeWidth="2.2" strokeLinecap="round"/></svg>
                  )}
                  {b.l}
                </div>
              ))}
            </div>
          </div>

          {/* 지금 혼자 식사 중 */}
          <div style={{
            marginTop: 28, padding: 18, borderRadius: 16,
            background: T2.brandSoft, border: `1px solid rgba(255,90,31,0.15)`,
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <div style={{ width: 8, height: 8, borderRadius: '50%', background: T2.brand, boxShadow: `0 0 0 4px rgba(255,90,31,0.2)` }} />
              <span style={{ fontSize: 12, fontWeight: 700, color: T2.brand, letterSpacing: 0.4, textTransform: 'uppercase' }}>지금 혼자 식사 중</span>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginTop: 14 }}>
              <div style={{ display: 'flex' }}>
                <Avatar name="민" bg="#171717" size={36} ring="#FFF4EF" />
                <div style={{ marginLeft: -8 }}><Avatar name="지" bg="#525252" size={36} ring="#FFF4EF" /></div>
              </div>
              <div style={{ flex: 1, fontSize: 13, color: T2.text, lineHeight: 1.4, fontWeight: 500 }}>
                <b>2명</b>이 같은 자리에서 식사 중이에요.<br/>
                <span style={{ color: T2.textSub }}>같이 먹기 신청을 보내볼까요?</span>
              </div>
              <div style={{
                alignSelf: 'center', padding: '10px 14px', borderRadius: 10,
                background: '#fff', color: T2.brand, fontSize: 13, fontWeight: 700,
                letterSpacing: -0.3, whiteSpace: 'nowrap', border: `1.5px solid ${T2.brand}`,
                cursor: 'pointer',
              }}>같이 먹기 →</div>
            </div>
          </div>

          {/* 정보 — 전화 · 영업시간 · 가격 · 홈페이지 */}
          <div style={{ marginTop: 28 }}>
            <div style={{ fontSize: 11, fontWeight: 700, color: T2.textMute, letterSpacing: 0.6, textTransform: 'uppercase', marginBottom: 12 }}>
              정보
            </div>
            {[
              { k: '전화', v: '02-322-1014', tel: true },
              { k: '영업시간', v: '매일 10:00 – 21:00 · 브레이크 15–17시' },
              { k: '홈페이지', v: 'instagram.com/keun_sundubu', link: true },
            ].map((r, i, arr) => (
              <div key={i} style={{
                display: 'flex', alignItems: 'center', gap: 16, padding: '12px 0',
                borderBottom: i < arr.length - 1 ? `1px solid ${T2.border}` : 'none',
              }}>
                <div style={{ width: 56, flexShrink: 0, fontSize: 13, fontWeight: 700, color: T2.textMute, letterSpacing: -0.2 }}>{r.k}</div>
                <div style={{ flex: 1, fontSize: 14, color: r.link ? T2.brand : T2.text, lineHeight: 1.5, letterSpacing: -0.3, textDecoration: r.link ? 'underline' : 'none', wordBreak: 'break-all' }}>{r.v}</div>
                {r.tel && (
                  <div style={{
                    flexShrink: 0, display: 'flex', alignItems: 'center', gap: 4, cursor: 'pointer',
                    padding: '6px 11px', borderRadius: 8, border: `1px solid ${T2.border}`, color: T2.text,
                  }}>
                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none">
                      <path d="M6.5 4h3l1.5 4-2 1.5a11 11 0 0 0 5 5l1.5-2 4 1.5v3a1.5 1.5 0 0 1-1.6 1.5C12 23 5 16 5 6.6A1.5 1.5 0 0 1 6.5 4z" stroke="currentColor" strokeWidth="1.7" strokeLinejoin="round"/>
                    </svg>
                    <span style={{ fontSize: 12, fontWeight: 700, letterSpacing: -0.2 }}>전화</span>
                  </div>
                )}
              </div>
            ))}
          </div>

          {/* 편의시설 */}
          <div style={{ marginTop: 24 }}>
            <div style={{ fontSize: 11, fontWeight: 700, color: T2.textMute, letterSpacing: 0.6, textTransform: 'uppercase', marginBottom: 12 }}>
              편의시설
            </div>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
              {[
                { l: '무료 와이파이', on: true },
                { l: '포장 가능', on: true },
                { l: '예약 가능', on: false },
                { l: '남녀 화장실 구분', on: true },
                { l: '주차', on: false },
                { l: '단체석', on: false },
              ].map((f, i) => (
                <div key={i} style={{
                  display: 'flex', alignItems: 'center', gap: 6,
                  padding: '9px 12px', borderRadius: 10,
                  background: f.on ? '#fff' : T2.bg,
                  border: `1px solid ${f.on ? T2.borderStrong : T2.border}`,
                  opacity: f.on ? 1 : 0.55,
                }}>
                  <span style={{ fontSize: 12, fontWeight: 800, color: f.on ? T2.brand : T2.textMute }}>{f.on ? '✓' : '–'}</span>
                  <span style={{ fontSize: 13, fontWeight: 600, color: f.on ? T2.text : T2.textMute, letterSpacing: -0.3 }}>{f.l}</span>
                </div>
              ))}
            </div>
          </div>
          </React.Fragment>
          )}

          {stab === 'menu' && (
            <div style={{ marginTop: 4 }}>
              {[
                { n: '얼큰순두부', d: '소·중·대', p: '9,000', best: true },
                { n: '굴순두부', d: '겨울 한정', p: '10,000', best: false },
                { n: '버섯들깨순두부', d: '담백한 맛', p: '9,500', best: false },
                { n: '순두부 정식', d: '+제육 1인', p: '12,000', best: false },
                { n: '공기밥 추가', d: '', p: '1,000', best: false },
              ].map((m, i, arr) => (
                <div key={i} style={{
                  display: 'flex', alignItems: 'center', gap: 14, padding: '14px 0',
                  borderBottom: i < arr.length - 1 ? `1px solid ${T2.border}` : 'none',
                }}>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                      <span style={{ fontSize: 15, fontWeight: 700, color: T2.text, letterSpacing: -0.3 }}>{m.n}</span>
                      {m.best && <span style={{ fontSize: 10, fontWeight: 700, color: '#fff', background: T2.brand, padding: '2px 6px', borderRadius: 5 }}>대표</span>}
                    </div>
                    {m.d && <div style={{ fontSize: 12, color: T2.textMute, marginTop: 4 }}>{m.d}</div>}
                  </div>
                  <span style={{ fontSize: 14, fontWeight: 700, color: T2.text, letterSpacing: -0.2, fontFeatureSettings: '"tnum"' }}>{m.p}원</span>
                </div>
              ))}
            </div>
          )}

          {stab === 'review' && (
            <div style={{ marginTop: 4 }}>
              {/* 리뷰 헤더 + 리뷰 쓰기 */}
              <div style={{ display: 'flex', alignItems: 'center', padding: '4px 0 16px' }}>
                <div style={{ flex: 1 }}>
                  <span style={{ fontSize: 15, fontWeight: 800, color: T2.text, letterSpacing: -0.4 }}>리뷰</span>
                  <span style={{ fontSize: 13, color: T2.textMute, marginLeft: 6, fontFeatureSettings: '"tnum"' }}>218개</span>
                </div>
                <div style={{
                  display: 'flex', alignItems: 'center', gap: 6,
                  padding: '9px 14px', borderRadius: 10, background: T2.brand,
                  color: '#fff', fontSize: 13, fontWeight: 700, letterSpacing: -0.3, cursor: 'pointer',
                }}>
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                    <path d="M4 20h4l10-10-4-4L4 16v4z" stroke="#fff" strokeWidth="1.8" strokeLinejoin="round"/>
                    <path d="M13.5 6.5l4 4" stroke="#fff" strokeWidth="1.8" strokeLinecap="round"/>
                  </svg>
                  리뷰 쓰기
                </div>
              </div>
              {[
                { u: '혼밥3년차', t: '바테이블이 벽 보고 앉는 구조라 진짜 편했어요. 눈치 안 보여서 자주 옵니다 🍲', taste: '5.0', honbab: '5.0', likes: 42, comments: 6, ago: '2일 전', hasPhoto: true },
                { u: '연남주민', t: '점심에 회전 빠르고 1인석 많아요. 순두부 간이 딱 좋음.', taste: '4.0', honbab: '4.5', likes: 18, comments: 2, ago: '5일 전', hasPhoto: true },
                { u: '국밥러버', t: '맛은 좋은데 저녁엔 사람이 많아서 혼밥은 조금 붐벼요.', taste: '4.0', honbab: '3.0', likes: 9, comments: 1, ago: '1주 전', hasPhoto: false },
              ].map((r, i, arr) => (
                <div key={i} style={{
                  paddingBottom: 18, marginBottom: 18,
                  borderBottom: i < arr.length - 1 ? `1px solid ${T2.border}` : 'none',
                }}>
                  {/* 게시물 헤더 */}
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                    <div style={{ padding: 2, borderRadius: '50%', background: `linear-gradient(135deg, ${T2.brand}, #FFB199)` }}>
                      <div style={{ padding: 2, borderRadius: '50%', background: '#fff' }}>
                        <Avatar name={r.u[0]} bg="#525252" size={32} />
                      </div>
                    </div>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ fontSize: 13, fontWeight: 800, color: T2.text, letterSpacing: -0.3 }}>{r.u}</div>
                      <div style={{ fontSize: 11, color: T2.textMute, marginTop: 1 }}>큰순두부 연남점</div>
                    </div>
                    <div style={{ display: 'flex', gap: 3, paddingRight: 2, cursor: 'pointer' }}>
                      {[0,1,2].map(d => <span key={d} style={{ width: 4, height: 4, borderRadius: '50%', background: T2.textMute }} />)}
                    </div>
                  </div>

                  {/* 사진 */}
                  {r.hasPhoto ? (
                    <div style={{ position: 'relative', marginTop: 12 }}>
                      <ImagePlaceholder w="100%" h={300} radius={14} bg="#EEE9DF" stripe="#E0D9C7" color="#A39B85" />
                      {/* 평점 태그 오버레이 */}
                      <div style={{ position: 'absolute', left: 12, bottom: 12, display: 'flex', gap: 6 }}>
                        <span style={{
                          display: 'flex', alignItems: 'center', gap: 4, padding: '5px 9px', borderRadius: 999,
                          background: 'rgba(10,10,10,0.62)', backdropFilter: 'blur(6px)', WebkitBackdropFilter: 'blur(6px)',
                          fontSize: 11, fontWeight: 700, color: '#fff', letterSpacing: -0.2,
                        }}>맛 ★ {r.taste}</span>
                        <span style={{
                          display: 'flex', alignItems: 'center', gap: 4, padding: '5px 9px', borderRadius: 999,
                          background: 'rgba(255,90,31,0.88)', backdropFilter: 'blur(6px)', WebkitBackdropFilter: 'blur(6px)',
                          fontSize: 11, fontWeight: 700, color: '#fff', letterSpacing: -0.2,
                        }}>혼밥 친화 ★ {r.honbab}</span>
                      </div>
                    </div>
                  ) : (
                    /* 사진 없는 글 — 평점 칩만 */
                    <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
                      <span style={{ display: 'flex', alignItems: 'center', gap: 5, padding: '5px 9px', borderRadius: 8, background: T2.bg, border: `1px solid ${T2.border}`, fontSize: 12, fontWeight: 800, color: T2.text, letterSpacing: -0.2 }}>맛 ★ {r.taste}</span>
                      <span style={{ display: 'flex', alignItems: 'center', gap: 5, padding: '5px 9px', borderRadius: 8, background: T2.brandSoft, border: `1px solid rgba(255,90,31,0.18)`, fontSize: 12, fontWeight: 800, color: T2.brand, letterSpacing: -0.2 }}>혼밥 친화 ★ {r.honbab}</span>
                    </div>
                  )}

                  {/* 액션 바 */}
                  <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginTop: 12 }}>
                    {/* 하트 */}
                    <svg width="24" height="24" viewBox="0 0 24 24" fill={i === 0 ? T2.brand : 'none'} style={{ cursor: 'pointer' }}>
                      <path d="M12 20.5l-1.45-1.32C5.4 14.5 2 11.4 2 7.6 2 4.7 4.4 2.5 7.2 2.5c1.6 0 3.1.74 4.05 1.9.95-1.16 2.45-1.9 4.05-1.9C18.1 2.5 20.5 4.7 20.5 7.6c0 3.8-3.4 6.9-8.55 11.58L12 20.5z" stroke={i === 0 ? T2.brand : T2.text} strokeWidth="1.7" strokeLinejoin="round"/>
                    </svg>
                    {/* 댓글 */}
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" style={{ cursor: 'pointer' }}>
                      <path d="M21 11.5a8.4 8.4 0 01-9 8.4 9 9 0 01-3.5-.6L3 21l1.4-4.2A8.4 8.4 0 1121 11.5z" stroke={T2.text} strokeWidth="1.7" strokeLinejoin="round"/>
                    </svg>
                    {/* 공유 */}
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" style={{ cursor: 'pointer' }}>
                      <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z" stroke={T2.text} strokeWidth="1.7" strokeLinejoin="round"/>
                    </svg>
                    <div style={{ flex: 1 }} />
                    {/* 저장 */}
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" style={{ cursor: 'pointer' }}>
                      <path d="M6 3h12a1 1 0 011 1v17l-7-4-7 4V4a1 1 0 011-1z" stroke={T2.text} strokeWidth="1.7" strokeLinejoin="round"/>
                    </svg>
                  </div>

                  {/* 좋아요 */}
                  <div style={{ fontSize: 13, fontWeight: 800, color: T2.text, marginTop: 10, letterSpacing: -0.3, fontFeatureSettings: '"tnum"' }}>좋아요 {r.likes}개</div>

                  {/* 캡션 */}
                  <div style={{ fontSize: 13, color: T2.text, lineHeight: 1.6, marginTop: 5, letterSpacing: -0.3 }}>
                    <span style={{ fontWeight: 800 }}>{r.u}</span> {r.t}
                  </div>

                  {/* 댓글 보기 + 시간 */}
                  <div style={{ fontSize: 12, color: T2.textMute, marginTop: 6, letterSpacing: -0.2 }}>댓글 {r.comments}개 모두 보기</div>
                  <div style={{ fontSize: 11, color: T2.textMute, marginTop: 5, letterSpacing: 0.2 }}>{r.ago}</div>
                </div>
              ))}
            </div>
          )}

          {stab === 'photo' && (
            <div style={{
              marginTop: 8, display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 6,
            }}>
              {Array.from({ length: 9 }).map((_, i) => (
                <ImagePlaceholder key={i} w="100%" h={110} radius={10} bg="#EEE9DF" stripe="#E0D9C7" color="#A39B85" />
              ))}
            </div>
          )}

          {stab === 'mate' && <DetailMateTab />}
          {stab === 'nearby' && <DetailNearbyTab />}
        </div>

        {/* 하단 CTA — 스크롤 영역 밖으로 빼서 항상 고정 */}
      </div>
      <div style={{
        position: 'absolute', left: 0, right: 0, bottom: 0, zIndex: 20,
        padding: '12px 16px 40px', background: '#fff',
        borderTop: `1px solid ${T2.border}`, display: 'flex', gap: 8, alignItems: 'stretch',
      }}>
        {/* 길찾기 — 아이콘 버튼 */}
        <div style={{
          width: 56, borderRadius: 12, background: T2.bg, flexShrink: 0,
          display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 3,
        }}>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
            <path d="M12 2L2 12l10 10 10-10L12 2z" fill="none" stroke={T2.text} strokeWidth="1.6" strokeLinejoin="round"/>
            <path d="M9 13v-2.5a1.5 1.5 0 0 1 1.5-1.5H14m0 0l-1.8-1.8M14 9l-1.8 1.8" stroke={T2.text} strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round"/>
          </svg>
          <span style={{ fontSize: 10, fontWeight: 700, color: T2.textSub, letterSpacing: -0.2 }}>길찾기</span>
        </div>

        {/* 같이 먹기 — 보조 버튼 */}
        <div style={{
          flex: 1, borderRadius: 12, background: '#fff', border: `1.5px solid ${T2.brand}`,
          color: T2.brand, fontSize: 14, fontWeight: 700, letterSpacing: -0.3,
          display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6, cursor: 'pointer',
        }}>
          <svg width="17" height="17" viewBox="0 0 24 24" fill="none">
            <circle cx="9" cy="8" r="3" stroke={T2.brand} strokeWidth="1.7"/>
            <path d="M3.5 19a5.5 5.5 0 0 1 11 0M16 6.5a3 3 0 0 1 0 5M17 19c0-2-1-3.6-2.5-4.5" stroke={T2.brand} strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round"/>
          </svg>
          같이 먹기
        </div>

        {/* 혼밥 시작 / 혼밥 중 — 주 버튼 (토글) */}
        {honbabOn ? (
          <div
            onClick={() => setHonbabOn(false)}
            style={{
              flex: 1, padding: '16px 0', borderRadius: 12, background: '#fff',
              color: T2.brand, fontSize: 14, fontWeight: 800, letterSpacing: -0.3,
              border: `1.5px solid ${T2.brand}`, cursor: 'pointer',
              display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 7,
            }}>
            <span style={{ position: 'relative', width: 8, height: 8 }}>
              <span style={{ position: 'absolute', inset: -3, borderRadius: '50%', background: T2.brand, opacity: 0.25 }} />
              <span style={{ position: 'absolute', inset: 0, borderRadius: '50%', background: T2.brand }} />
            </span>
            혼밥 중
          </div>
        ) : (
          <div
            onClick={() => setHonbabOn(true)}
            style={{
              flex: 1, padding: '16px 0', borderRadius: 12, background: T2.brand,
              color: '#fff', fontSize: 14, fontWeight: 700, letterSpacing: -0.3,
              border: '1.5px solid transparent',
              textAlign: 'center', cursor: 'pointer',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}>혼밥 시작</div>
        )}
      </div>
    </PhoneShell>
  );
}

// ───────────────────────────────────────────────────────────
// 메이트 탭 — 이 식당을 다녀간 / 담은 메이트 (사회적 신뢰)
// ───────────────────────────────────────────────────────────
function DetailMateTab() {
  const liveMates = [
    { n: '지현', init: '지', bg: '#171717', tag: '대화 환영', mutual: '같이 3회', here: '바테이블 · 12분째' },
  ];
  const visited = [
    { n: '연남또일이', init: '연', bg: '#525252', mood: '조용히', together: 2, visits: 4, score: '4.8', last: '3일 전' },
    { n: '순두부조아', init: '순', bg: '#7C7C7C', mood: '대화 환영', together: 1, visits: 6, score: '4.5', last: '1주 전' },
    { n: '혼밥부장', init: '혼', bg: '#171717', mood: '대화 환영', together: 0, visits: 3, score: '4.6', last: '2주 전' },
  ];
  const saved = [
    { init: '미', bg: '#171717' }, { init: '도', bg: '#525252' },
    { init: '하', bg: '#7C7C7C' }, { init: '진', bg: '#A3A3A3' },
  ];

  const Section = ({ title, count, children }) => (
    <div style={{ marginTop: 26 }}>
      <div style={{ display: 'flex', alignItems: 'baseline', gap: 7, marginBottom: 12 }}>
        <span style={{ fontSize: 11, fontWeight: 700, color: T2.textMute, letterSpacing: 0.6, textTransform: 'uppercase' }}>{title}</span>
        {count != null && <span style={{ fontSize: 12, fontWeight: 700, color: T2.textMute, fontFeatureSettings: '"tnum"' }}>{count}</span>}
      </div>
      {children}
    </div>
  );

  return (
    <div style={{ marginTop: 8 }}>
      {/* 요약 배너 — 사회적 신뢰 */}
      <div style={{
        marginTop: 18, padding: 16, borderRadius: 16,
        background: '#fff', border: `1px solid ${T2.border}`,
        display: 'flex', alignItems: 'center', gap: 13,
      }}>
        <div style={{ display: 'flex' }}>
          {['민','도','하'].map((c, i) => (
            <div key={i} style={{ marginLeft: i ? -10 : 0 }}>
              <Avatar name={c} bg={['#171717','#525252','#7C7C7C'][i]} size={36} ring="#fff" />
            </div>
          ))}
        </div>
        <div style={{ flex: 1, fontSize: 13.5, color: T2.text, lineHeight: 1.45, fontWeight: 500, letterSpacing: -0.3 }}>
          내 메이트 <b>3명</b>이 여기 다녀갔어요.<br/>
          <span style={{ color: T2.textSub }}>믿고 혼밥하기 좋은 곳이에요.</span>
        </div>
      </div>

      {/* 지금 여기서 혼밥 중 */}
      <Section title="지금 여기서 혼밥 중" count={liveMates.length}>
        {liveMates.map((m, i) => (
          <div key={i} style={{
            display: 'flex', alignItems: 'center', gap: 12, padding: 14, borderRadius: 14,
            background: T2.brandSoft, border: `1px solid rgba(255,90,31,0.15)`,
          }}>
            <div style={{ position: 'relative' }}>
              <Avatar name={m.init} bg={m.bg} size={44} ring="#FFF4EF" />
              <span style={{ position: 'absolute', right: -1, bottom: -1, width: 13, height: 13, borderRadius: '50%', background: T2.brand, border: '2.5px solid #FFF4EF' }} />
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                <span style={{ fontSize: 15, fontWeight: 800, color: T2.text, letterSpacing: -0.4 }}>{m.n}</span>
                <span style={{ fontSize: 10, fontWeight: 700, color: T2.brand, background: '#fff', padding: '2px 6px', borderRadius: 5, border: `1px solid rgba(255,90,31,0.25)` }}>메이트</span>
              </div>
              <div style={{ fontSize: 12, color: T2.textSub, marginTop: 3, letterSpacing: -0.2 }}>{m.here} · {m.mutual}</div>
            </div>
            <div style={{
              alignSelf: 'center', padding: '9px 14px', borderRadius: 10, background: T2.brand,
              color: '#fff', fontSize: 13, fontWeight: 700, letterSpacing: -0.3, whiteSpace: 'nowrap', cursor: 'pointer',
            }}>같이 먹기</div>
          </div>
        ))}
      </Section>

      {/* 다녀온 메이트 */}
      <Section title="다녀온 메이트" count={visited.length}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          {visited.map((m, i, arr) => (
            <div key={i} style={{
              display: 'flex', alignItems: 'center', gap: 12, padding: '12px 0',
              borderBottom: i < arr.length - 1 ? `1px solid ${T2.border}` : 'none',
            }}>
              <Avatar name={m.init} bg={m.bg} size={40} />
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <span style={{ fontSize: 14.5, fontWeight: 700, color: T2.text, letterSpacing: -0.4 }}>{m.n}</span>
                  <span style={{ fontSize: 11, fontWeight: 600, color: T2.textMute, background: T2.bg, padding: '2px 7px', borderRadius: 999, border: `1px solid ${T2.border}` }}>{m.mood}</span>
                </div>
                <div style={{ fontSize: 12, color: T2.textMute, marginTop: 4, letterSpacing: -0.2, fontFeatureSettings: '"tnum"' }}>
                  {m.together > 0 ? <span style={{ color: T2.brand, fontWeight: 700 }}>같이 {m.together}회 · </span> : null}
                  방문 {m.visits}회 · 혼밥친화 ★{m.score} · {m.last}
                </div>
              </div>
              <div style={{
                flexShrink: 0, padding: '8px 12px', borderRadius: 9, background: '#fff',
                border: `1.5px solid ${m.together > 0 ? T2.brand : T2.border}`,
                color: m.together > 0 ? T2.brand : T2.textSub,
                fontSize: 12.5, fontWeight: 700, letterSpacing: -0.3, whiteSpace: 'nowrap', cursor: 'pointer',
              }}>{m.together > 0 ? '같이 먹기' : '메이트 신청'}</div>
            </div>
          ))}
        </div>
      </Section>

      {/* 즐겨찾기에 담은 메이트 */}
      <Section title="즐겨찾기에 담은 메이트" count={12}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <div style={{ display: 'flex' }}>
            {saved.map((s, i) => (
              <div key={i} style={{ marginLeft: i ? -10 : 0 }}>
                <Avatar name={s.init} bg={s.bg} size={38} ring="#FAFAF7" />
              </div>
            ))}
            <div style={{ marginLeft: -10, width: 38, height: 38, borderRadius: '50%', background: '#fff', border: `1px solid ${T2.border}`, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 12, fontWeight: 700, color: T2.textSub, boxShadow: '0 0 0 2px #FAFAF7' }}>+8</div>
          </div>
          <div style={{ flex: 1, fontSize: 13, color: T2.textSub, letterSpacing: -0.3, lineHeight: 1.4 }}>
            메이트 <b style={{ color: T2.text }}>4명</b>을 포함해<br/>12명이 이 식당을 저장했어요
          </div>
        </div>
      </Section>
    </div>
  );
}

// ───────────────────────────────────────────────────────────
// 주변 탭 — 카카오 로컬 기준 근처 혼밥 좋은 곳
// ───────────────────────────────────────────────────────────
function DetailNearbyTab() {
  const [filter, setFilter] = React.useState('honbab');
  const data = {
    honbab: [
      { n: '연남 김밥상회', cat: '분식 · 김밥', walk: '도보 2분', dist: '140m', score: '4.7', tag: '1인석' },
      { n: '혼밀라멘 연남', cat: '일식 · 라멘', walk: '도보 4분', dist: '260m', score: '4.6', tag: '바테이블' },
      { n: '오늘의 덮밥', cat: '한식 · 덮밥', walk: '도보 5분', dist: '320m', score: '4.5', tag: '칸막이' },
      { n: '연남 우동집', cat: '일식 · 우동', walk: '도보 6분', dist: '400m', score: '4.3', tag: '1인석' },
    ],
    cafe: [
      { n: '연남 로스터스', cat: '카페 · 디저트', walk: '도보 1분', dist: '90m', score: '4.6', tag: '콘센트' },
      { n: '책읽는 고양이', cat: '북카페', walk: '도보 3분', dist: '210m', score: '4.4', tag: '조용함' },
      { n: '느린오후', cat: '카페', walk: '도보 5분', dist: '330m', score: '4.2', tag: '1인석' },
    ],
    parking: [
      { n: '연남공영주차장', cat: '공영 · 시간당 1,200원', walk: '도보 3분', dist: '200m', score: '여유', tag: '32면' },
      { n: '연남로 노상주차', cat: '노상 · 시간당 1,000원', walk: '도보 2분', dist: '150m', score: '혼잡', tag: '8면' },
    ],
  };
  const tabs = [
    { key: 'honbab', label: '혼밥 맛집' },
    { key: 'cafe', label: '카페' },
    { key: 'parking', label: '주차' },
  ];
  const rows = data[filter];

  return (
    <div style={{ marginTop: 12 }}>
      {/* 안내 */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 14 }}>
        <svg width="13" height="13" viewBox="0 0 24 24" fill="none">
          <path d="M12 2C8.1 2 5 5.1 5 9c0 5.2 7 13 7 13s7-7.8 7-13c0-3.9-3.1-7-7-7z" stroke={T2.textMute} strokeWidth="1.8" strokeLinejoin="round"/>
          <circle cx="12" cy="9" r="2.2" stroke={T2.textMute} strokeWidth="1.8"/>
        </svg>
        <span style={{ fontSize: 12, color: T2.textMute, letterSpacing: -0.2 }}>큰순두부 연남점 주변 · 카카오맵 기준</span>
      </div>

      {/* 필터 칩 */}
      <div style={{ display: 'flex', gap: 8, marginBottom: 4 }}>
        {tabs.map((t) => {
          const on = filter === t.key;
          return (
            <div key={t.key} onClick={() => setFilter(t.key)} style={{
              padding: '8px 15px', borderRadius: 999, cursor: 'pointer',
              fontSize: 13, fontWeight: 700, letterSpacing: -0.3,
              background: on ? T2.text : '#fff',
              color: on ? '#fff' : T2.textSub,
              border: `1px solid ${on ? T2.text : T2.border}`,
            }}>{t.label}</div>
          );
        })}
      </div>

      {/* 리스트 */}
      <div style={{ marginTop: 6 }}>
        {rows.map((r, i, arr) => (
          <div key={i} style={{
            display: 'flex', alignItems: 'center', gap: 14, padding: '14px 0',
            borderBottom: i < arr.length - 1 ? `1px solid ${T2.border}` : 'none', cursor: 'pointer',
          }}>
            <ImagePlaceholder w={56} h={56} radius={12} bg="#EEE9DF" stripe="#E0D9C7" color="#A39B85" tag="" />
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 15, fontWeight: 700, color: T2.text, letterSpacing: -0.4 }}>{r.n}</div>
              <div style={{ fontSize: 12, color: T2.textMute, marginTop: 3, letterSpacing: -0.2 }}>{r.cat}</div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 7, marginTop: 7 }}>
                <span style={{ fontSize: 12, fontWeight: 700, color: T2.text, letterSpacing: -0.2, fontFeatureSettings: '"tnum"' }}>{r.walk}</span>
                <span style={{ width: 2, height: 2, borderRadius: '50%', background: T2.textMute }} />
                <span style={{ fontSize: 12, color: T2.textMute, fontFeatureSettings: '"tnum"' }}>{r.dist}</span>
                <span style={{
                  marginLeft: 2, padding: '2px 8px', borderRadius: 999, fontSize: 11, fontWeight: 700, letterSpacing: -0.2,
                  background: T2.brandSoft, color: T2.brand, border: `1px solid rgba(255,90,31,0.18)`,
                }}>{r.tag}</span>
              </div>
            </div>
            <div style={{ textAlign: 'right', flexShrink: 0 }}>
              {filter === 'parking' ? (
                <span style={{
                  fontSize: 12, fontWeight: 800, letterSpacing: -0.2,
                  color: r.score === '여유' ? '#1F8A5B' : T2.brand,
                }}>{r.score}</span>
              ) : (
                <React.Fragment>
                  <div style={{ fontSize: 16, fontWeight: 800, color: T2.text, letterSpacing: -0.4, fontFeatureSettings: '"tnum"' }}>{r.score}</div>
                  <div style={{ fontSize: 10, color: T2.textMute, marginTop: 1 }}>혼밥친화</div>
                </React.Fragment>
              )}
            </div>
          </div>
        ))}
      </div>

      {/* 지도에서 보기 */}
      <div style={{
        marginTop: 18, padding: '14px 0', borderRadius: 12, background: '#fff',
        border: `1px solid ${T2.border}`, textAlign: 'center',
        fontSize: 13.5, fontWeight: 700, color: T2.text, letterSpacing: -0.3, cursor: 'pointer',
        display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 7,
      }}>
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
          <path d="M9 3L3 6v15l6-3 6 3 6-3V3l-6 3-6-3z" stroke={T2.text} strokeWidth="1.6" strokeLinejoin="round"/>
          <path d="M9 3v15M15 6v15" stroke={T2.text} strokeWidth="1.6"/>
        </svg>
        지도에서 한눈에 보기
      </div>
    </div>
  );
}

Object.assign(window, { RestaurantDetail });
