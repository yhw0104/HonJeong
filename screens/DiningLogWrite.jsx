// DiningLogWrite.jsx — 혼밥 인증 작성
// (구 Min_Journal)

// ───────────────────────────────────────────────────────────
// 화면 3: 혼밥 인증 — 저널 스타일
// ───────────────────────────────────────────────────────────
function DiningLogWrite() {
  return (
    <PhoneShell bg={T2.bg}>
      {/* 헤더 */}
      <div style={{
        position: 'absolute', top: 56, left: 0, right: 0, height: 52,
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        padding: '0 20px', zIndex: 10,
      }}>
        <div style={{ fontSize: 14, fontWeight: 600, color: T2.textSub, letterSpacing: -0.2 }}>닫기</div>
        <div style={{ fontSize: 14, fontWeight: 700, color: T2.brand, letterSpacing: -0.2 }}>저장</div>
      </div>

      <div style={{ position: 'absolute', top: 108, left: 0, right: 0, bottom: 0, overflow: 'auto', padding: '0 20px 40px' }}>
        {/* 날짜 / 메타 — 큰 타이포 */}
        <div style={{ fontSize: 11, fontWeight: 700, color: T2.textMute, letterSpacing: 1, textTransform: 'uppercase' }}>
          2026.05.22 · FRI · 12:34
        </div>
        <h1 style={{
          fontSize: 36, fontWeight: 800, color: T2.text, letterSpacing: -1.2,
          margin: '4px 0 0', lineHeight: 1.05,
        }}>
          오늘의 <br/>
          <span style={{ color: T2.brand }}>32번째</span> 혼밥
        </h1>

        {/* 장소 */}
        <div style={{
          display: 'flex', alignItems: 'center', gap: 10, padding: '14px 0', marginTop: 20,
          borderTop: `1px solid ${T2.border}`, borderBottom: `1px solid ${T2.border}`,
        }}>
          <div style={{ width: 6, height: 6, borderRadius: '50%', background: T2.brand }} />
          <div style={{ flex: 1, fontSize: 14, fontWeight: 600, color: T2.text, letterSpacing: -0.3 }}>
            큰순두부 연남점
          </div>
          <div style={{ fontSize: 12, color: T2.textMute }}>마포구 · 한식</div>
        </div>

        {/* 사진 */}
        <div style={{ marginTop: 20, display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 6 }}>
          <div style={{ gridColumn: 'span 2', gridRow: 'span 2' }}>
            <ImagePlaceholder w="100%" h={232} radius={10} bg="#EEE9DF" stripe="#E0D9C7" color="#A39B85" tag="순두부 한 그릇" />
          </div>
          <ImagePlaceholder w="100%" h={113} radius={10} bg="#EEE9DF" stripe="#E0D9C7" color="#A39B85" tag="2" />
          <div style={{
            width: '100%', height: 113, borderRadius: 10, border: `1.5px dashed ${T2.borderStrong}`,
            display: 'flex', alignItems: 'center', justifyContent: 'center', color: T2.textMute, fontSize: 22,
          }}>+</div>
        </div>

        {/* 기분 — 컴팩트 셀렉트 */}
        <div style={{ marginTop: 28 }}>
          <div style={{ fontSize: 12, fontWeight: 700, color: T2.textMute, letterSpacing: 0.5, textTransform: 'uppercase' }}>오늘의 기분</div>
          <div style={{
            marginTop: 12, display: 'flex', gap: 8, flexWrap: 'wrap',
          }}>
            {['편안', '행복', '맛있음', '집중', '조금 어색', '쓸쓸'].map((m, i) => (
              <div key={i} style={{
                padding: '9px 14px', borderRadius: 999,
                background: i === 0 ? T2.text : '#fff',
                color: i === 0 ? '#fff' : T2.text,
                border: i === 0 ? 'none' : `1px solid ${T2.border}`,
                fontSize: 13, fontWeight: 600, letterSpacing: -0.2,
              }}>{m}</div>
            ))}
          </div>
        </div>

        {/* 친화도 */}
        {/* 두 갈래 별점 — 맛 평가(→리뷰) + 혼밥 친화도(→친화도) */}
        <div style={{
          marginTop: 28, padding: 18, borderRadius: 14, background: '#fff',
          border: `1px solid ${T2.border}`,
        }}>
          {/* 맛 평가 */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 14, fontWeight: 800, color: T2.text, letterSpacing: -0.3 }}>다시 방문하고 싶은 곳인가요?</div>
              <div style={{ fontSize: 11, color: T2.textMute, marginTop: 2 }}>가게 리뷰 별점에 반영돼요</div>
            </div>
            <div style={{ display: 'flex', gap: 4 }}>
              {[1,2,3,4,5].map(s => (
                <span key={s} style={{ fontSize: 24, lineHeight: 1, color: s <= 4 ? T2.brand : T2.border }}>★</span>
              ))}
            </div>
          </div>

          <div style={{ height: 1, background: T2.border, margin: '16px 0' }} />

          {/* 혼밥 친화도 */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 14, fontWeight: 800, color: T2.text, letterSpacing: -0.3 }}>혼밥하기는 어땠나요?</div>
              <div style={{ fontSize: 11, color: T2.textMute, marginTop: 2 }}>혼밥 친화도에 반영돼요</div>
            </div>
            <div style={{ display: 'flex', gap: 4 }}>
              {[1,2,3,4,5].map(s => (
                <span key={s} style={{ fontSize: 24, lineHeight: 1, color: s <= 5 ? T2.brand : T2.border }}>★</span>
              ))}
            </div>
          </div>

          {/* 친화 요소 태그 — 혼밥 친화도 보조 */}
          <div style={{ marginTop: 16, display: 'flex', gap: 6, flexWrap: 'wrap' }}>
            {[
              { l: '1인석 많음', on: true },
              { l: '바테이블', on: true },
              { l: '칸막이', on: false },
              { l: '눈치 없음', on: true },
              { l: '오래 OK', on: false },
            ].map((b, i) => (
              <div key={i} style={{
                padding: '7px 12px', borderRadius: 999, fontSize: 12, fontWeight: 600, letterSpacing: -0.2,
                background: b.on ? T2.brand : '#fff', color: b.on ? '#fff' : T2.textMute,
                border: `1px solid ${b.on ? T2.brand : T2.border}`,
              }}>{b.l}</div>
            ))}
          </div>
        </div>

        {/* 본문 */}
        <div style={{ marginTop: 28 }}>
          <div style={{ fontSize: 12, fontWeight: 700, color: T2.textMute, letterSpacing: 0.5, textTransform: 'uppercase' }}>
            한 줄 기록
          </div>
          <div style={{ marginTop: 14, fontSize: 17, color: T2.text, lineHeight: 1.7, letterSpacing: -0.3 }}>
            창가 바테이블에서 순두부 한 그릇.<br/>
            점심에 1인석이 절반이나 비어있어서 눈치 볼 일이 없었다.<br/>
            <span style={{ color: T2.textMute }}>다음엔 비빔밥도 시켜봐야지.</span>
            <span style={{
              display: 'inline-block', width: 1.5, height: 18, background: T2.text,
              verticalAlign: 'text-bottom', marginLeft: 2,
            }} />
          </div>
        </div>

      </div>
    </PhoneShell>
  );
}

Object.assign(window, { DiningLogWrite });
