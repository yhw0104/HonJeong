// MapHome.jsx — 지도 / 홈
// (구 Min_Map)

// ───────────────────────────────────────────────────────────
// 화면 1: 지도 (홈)
// ───────────────────────────────────────────────────────────
function MapHome() {
  const [honbabOn, setHonbabOn] = React.useState(false);
  const [picking, setPicking] = React.useState(false);
  const [placeName, setPlaceName] = React.useState('큰순두부 연남점');

  const nearby = [
    { n: '큰순두부 연남점', d: '한식 · 120m', tag: '메이트 2명' },
    { n: '혼밥의자', d: '일식 · 180m', tag: '바테이블 6석' },
    { n: '옥상국밥', d: '한식 · 240m', tag: '1인석 가능' },
    { n: '연남 파스타바', d: '양식 · 300m', tag: '오래 OK' },
  ];

  const startHonbab = (name) => { setPlaceName(name); setPicking(false); setHonbabOn(true); };
  return (
    <PhoneShell bg={T2.bg}>
      <MapBackground theme={T2} />

      {/* 혼밥 중 상태 바 — 내가 혼밥 시작을 켜두면 앱 어디서나 표시 */}
      {honbabOn && <HonbabStatusBar place={placeName} onEnd={() => setHonbabOn(false)} />}

      {/* 상단 검색 + 길찾기 */}
      <div style={{ position: 'absolute', top: honbabOn ? 118 : 60, left: 16, right: 16, zIndex: 10, transition: 'top 0.2s' }}>
        <div style={{ display: 'flex', gap: 8 }}>
          {/* 검색창 */}
          <div style={{
            flex: 1, background: '#fff', borderRadius: 14, height: 48,
            display: 'flex', alignItems: 'center', padding: '0 14px', gap: 10,
            boxShadow: '0 2px 12px rgba(0,0,0,0.06), 0 0 0 1px rgba(0,0,0,0.04)',
          }}>
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
              <circle cx="7" cy="7" r="5" stroke={T2.text} strokeWidth="1.5"/>
              <path d="M11 11l3 3" stroke={T2.text} strokeWidth="1.5" strokeLinecap="round"/>
            </svg>
            <div style={{ flex: 1, color: T2.textMute, fontSize: 14, letterSpacing: -0.3 }}>
              장소, 음식, 메이트
            </div>
          </div>
          {/* 길찾기 버튼 */}
          <div style={{
            width: 48, height: 48, borderRadius: 14, background: T2.brand,
            display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 1,
            boxShadow: '0 4px 12px rgba(255,90,31,0.3)', flexShrink: 0,
          }}>
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
              <path d="M12 2L2 12l10 10 10-10L12 2z" fill="none" stroke="#fff" strokeWidth="1.6" strokeLinejoin="round"/>
              <path d="M9 13v-2.5a1.5 1.5 0 0 1 1.5-1.5H14m0 0l-1.8-1.8M14 9l-1.8 1.8" stroke="#fff" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round"/>
            </svg>
          </div>
        </div>

        {/* 라이브 카운터 — 한 줄 */}
        <div style={{
          marginTop: 10, padding: '10px 14px', background: '#fff', borderRadius: 12,
          display: 'flex', alignItems: 'center', gap: 10,
          boxShadow: '0 2px 12px rgba(0,0,0,0.06), 0 0 0 1px rgba(0,0,0,0.04)',
        }}>
          <div style={{ position: 'relative', width: 8, height: 8, flexShrink: 0 }}>
            <div style={{
              position: 'absolute', inset: -4, borderRadius: '50%', background: T2.brand, opacity: 0.18,
            }} />
            <div style={{ position: 'absolute', inset: 0, borderRadius: '50%', background: T2.brand }} />
          </div>
          <div style={{ fontSize: 12, fontWeight: 700, color: T2.text, letterSpacing: -0.2, lineHeight: 1 }}>
            지금 연남동에서 혼밥 중
          </div>
          <div style={{
            marginLeft: 'auto', fontSize: 16, fontWeight: 800, color: T2.text,
            fontFeatureSettings: '"tnum"', letterSpacing: -0.5, lineHeight: 1,
          }}>27<span style={{ fontSize: 11, fontWeight: 600, color: T2.textMute, marginLeft: 2 }}>명</span></div>
        </div>

      </div>

      {/* 핀 */}
      <MiniPin x={120} y={320} label="큰순두부" mate />
      <MiniPin x={250} y={280} active />
      <MiniPin x={180} y={420} />
      <MiniPin x={290} y={500} label="혼밥하우스 · 메이트 1" mate />
      <MiniPin x={90} y={520} />
      <MiniPin x={340} y={620} />

      {/* 내 위치 */}
      <div style={{
        position: 'absolute', left: 195, top: 480, transform: 'translate(-50%,-50%)', zIndex: 4,
        width: 14, height: 14, borderRadius: '50%', background: '#171717',
        border: '3px solid #fff', boxShadow: '0 0 0 8px rgba(23,23,23,0.08)',
      }} />

      {/* 컨트롤 */}
      <div style={{
        position: 'absolute', right: 16, bottom: 360, zIndex: 9,
        display: 'flex', flexDirection: 'column', borderRadius: 12, overflow: 'hidden',
        background: '#fff', boxShadow: '0 4px 12px rgba(0,0,0,0.08)',
      }}>
        {['+', '−', '◎'].map((s, i, a) => (
          <div key={i} style={{
            width: 40, height: 40, display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontSize: 18, fontWeight: 500, color: T2.text,
            borderBottom: i < a.length - 1 ? `1px solid ${T2.border}` : 'none',
          }}>{s}</div>
        ))}
      </div>

      {/* 하단 시트 — 깔끔한 리스트 */}
      <div style={{
        position: 'absolute', left: 0, right: 0, bottom: 78, zIndex: 8,
        background: '#fff', borderTopLeftRadius: 24, borderTopRightRadius: 24,
        boxShadow: '0 -4px 24px rgba(0,0,0,0.06)', paddingBottom: 8, paddingTop: 12,
      }}>
        <div style={{ width: 36, height: 4, background: '#E5E5E5', borderRadius: 2, margin: '0 auto 16px' }} />
        <div style={{
          padding: '0 20px 12px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12,
        }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <div style={{ position: 'relative', width: 7, height: 7 }}>
                <div style={{ position: 'absolute', inset: -3, borderRadius: '50%', background: T2.brand, opacity: 0.2 }} />
                <div style={{ position: 'absolute', inset: 0, borderRadius: '50%', background: T2.brand }} />
              </div>
              <div style={{ fontSize: 11, fontWeight: 700, color: T2.brand, letterSpacing: 0.5, textTransform: 'uppercase' }}>지금 · 실시간</div>
            </div>
            <div style={{ fontSize: 26, fontWeight: 800, color: T2.text, letterSpacing: -0.8, marginTop: 4, lineHeight: 1.1 }}>
              연남동에서 <span style={{ color: T2.brand }}>27명</span>이<br/>혼자 식사 중
            </div>
            <div style={{ fontSize: 12, color: T2.textMute, marginTop: 6, letterSpacing: -0.2 }}>
              메이트 모집 중 <b style={{ color: T2.text }}>3명</b> · 가게 8곳
            </div>
          </div>

          {/* 혼밥 시작 — 식당 선택으로 이어짐 */}
          {honbabOn ? (
            <div
              onClick={() => setHonbabOn(false)}
              style={{
                flexShrink: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 7,
                padding: '15px 18px', borderRadius: 12, background: '#fff', border: `1.5px solid ${T2.brand}`,
                cursor: 'pointer',
              }}>
              <span style={{ position: 'relative', width: 8, height: 8 }}>
                <span style={{ position: 'absolute', inset: -3, borderRadius: '50%', background: T2.brand, opacity: 0.25 }} />
                <span style={{ position: 'absolute', inset: 0, borderRadius: '50%', background: T2.brand }} />
              </span>
              <span style={{ fontSize: 14, fontWeight: 700, color: T2.brand, letterSpacing: -0.3, whiteSpace: 'nowrap' }}>혼밥 중</span>
            </div>
          ) : (
            <div
              onClick={() => setPicking(true)}
              style={{
                flexShrink: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6,
                padding: '15px 20px', borderRadius: 12, background: T2.brand,
                boxShadow: '0 6px 16px rgba(255,90,31,0.32)', cursor: 'pointer',
              }}>
              <span style={{ fontSize: 16, lineHeight: 1 }}>🍚</span>
              <span style={{ fontSize: 14, fontWeight: 700, color: '#fff', letterSpacing: -0.3, whiteSpace: 'nowrap' }}>혼밥 시작</span>
            </div>
          )}
        </div>

        {[
          { n: '큰순두부 연남점', d: '한식 · 120m', tag: '메이트 2명', star: '4.6', tagOn: true },
          { n: '혼밥의자', d: '일식 · 180m', tag: '바테이블 6석', star: '4.8' },
          { n: '옥상국밥', d: '한식 · 240m', tag: '1인석 가능', star: '4.3' },
        ].map((r, i) => (
          <div key={i} style={{
            display: 'flex', alignItems: 'center', gap: 12, padding: '12px 20px',
            borderTop: i === 0 ? `1px solid ${T2.border}` : 'none',
            borderBottom: `1px solid ${T2.border}`,
          }}>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 15, fontWeight: 700, color: T2.text, letterSpacing: -0.3 }}>{r.n}</div>
              <div style={{ fontSize: 12, color: T2.textSub, marginTop: 3, display: 'flex', gap: 6, alignItems: 'center' }}>
                <span>{r.d}</span>
                <span style={{ color: T2.textMute }}>·</span>
                <span style={{
                  color: r.tagOn ? T2.brand : T2.textSub,
                  fontWeight: r.tagOn ? 700 : 500,
                }}>{r.tagOn && '● '}{r.tag}</span>
              </div>
            </div>
            <div style={{ textAlign: 'right' }}>
              <div style={{ fontSize: 14, fontWeight: 700, color: T2.text, letterSpacing: -0.2, fontFeatureSettings: '"tnum"' }}>★ {r.star}</div>
            </div>
          </div>
        ))}
      </div>

      {/* 하단 탭 바 */}
      <MinTabBar active="home" />

      {/* 식당 선택 시트 — 혼밥 시작 전 어디서 먹는지 체크 */}
      {picking && (
        <React.Fragment>
          {/* 딘면 */}
          <div
            onClick={() => setPicking(false)}
            style={{ position: 'absolute', inset: 0, zIndex: 60, background: 'rgba(10,10,10,0.4)' }}
          />
          {/* 시트 */}
          <div style={{
            position: 'absolute', left: 0, right: 0, bottom: 0, zIndex: 61,
            background: '#fff', borderTopLeftRadius: 24, borderTopRightRadius: 24,
            paddingBottom: 40, paddingTop: 12, boxShadow: '0 -8px 30px rgba(0,0,0,0.18)',
          }}>
            <div style={{ width: 36, height: 4, background: '#E5E5E5', borderRadius: 2, margin: '0 auto 18px' }} />
            <div style={{ padding: '0 20px 4px' }}>
              <div style={{ fontSize: 20, fontWeight: 800, color: T2.text, letterSpacing: -0.5 }}>어디서 혼밥 중이세요?</div>
              <div style={{ fontSize: 13, color: T2.textMute, marginTop: 5, letterSpacing: -0.3 }}>선택한 식당에 ‘혼밥 중’으로 표시돼요</div>
            </div>
            <div style={{ marginTop: 14 }}>
              {nearby.map((p, i) => (
                <div key={i}
                  onClick={() => startHonbab(p.n)}
                  style={{
                    display: 'flex', alignItems: 'center', gap: 13, padding: '14px 20px',
                    borderTop: i === 0 ? `1px solid ${T2.border}` : 'none',
                    borderBottom: `1px solid ${T2.border}`, cursor: 'pointer',
                  }}>
                  <div style={{
                    width: 44, height: 44, borderRadius: 12, flexShrink: 0,
                    background: T2.bg, border: `1px solid ${T2.border}`,
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    fontSize: 18, color: T2.textMute,
                  }}>🍽</div>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: 15, fontWeight: 700, color: T2.text, letterSpacing: -0.3 }}>{p.n}</div>
                    <div style={{ fontSize: 12, color: T2.textSub, marginTop: 3, letterSpacing: -0.2 }}>{p.d} · {p.tag}</div>
                  </div>
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" style={{ flexShrink: 0 }}>
                    <path d="M9 6l6 6-6 6" stroke={T2.textMute} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                  </svg>
                </div>
              ))}
            </div>
            {/* 직접 검색 */}
            <div style={{
              margin: '16px 20px 0', padding: '14px', borderRadius: 12,
              border: `1.5px dashed ${T2.borderStrong}`, textAlign: 'center',
              fontSize: 14, fontWeight: 700, color: T2.textSub, letterSpacing: -0.3, cursor: 'pointer',
            }}>직접 검색해서 찾기</div>
          </div>
        </React.Fragment>
      )}
    </PhoneShell>
  );
}

Object.assign(window, { MapHome });
