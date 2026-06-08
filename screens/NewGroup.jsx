// NewGroup.jsx — 새 그룹 만들기
// (구 Min_NewGroup)

// ───────────────────────────────────────────────────────────
// 화면: 새 그룹 만들기 (즐겨찾기 → "새 그룹 만들기" 진입)
// ───────────────────────────────────────────────────────────
function NewGroup() {
  const emojis = ['🍜', '🍣', '🍲', '🍙', '🍛', '🍔', '☕️', '🍱'];
  const sel = 0;

  return (
    <PhoneShell bg={T2.bg}>
      {/* 상단 바 */}
      <div style={{
        position: 'absolute', top: 54, left: 0, right: 0, height: 52, zIndex: 10,
        display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 20px',
      }}>
        <span style={{ fontSize: 15, fontWeight: 600, color: T2.textSub, letterSpacing: -0.3, cursor: 'pointer' }}>취소</span>
        <span style={{ fontSize: 16, fontWeight: 800, color: T2.text, letterSpacing: -0.3 }}>새 그룹</span>
        <span style={{ fontSize: 15, fontWeight: 800, color: T2.brand, letterSpacing: -0.3, cursor: 'pointer' }}>완료</span>
      </div>

      <div style={{ position: 'absolute', top: 118, left: 0, right: 0, bottom: 0, overflow: 'auto', padding: '0 20px 32px' }}>
        {/* 아이콘 + 이름 */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 14, marginTop: 8 }}>
          <div style={{
            width: 60, height: 60, borderRadius: 16, flexShrink: 0,
            background: '#fff', border: `1px solid ${T2.border}`,
            display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 28,
            boxShadow: '0 1px 4px rgba(0,0,0,0.05)',
          }}>{emojis[sel]}</div>
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 11, fontWeight: 700, color: T2.textMute, letterSpacing: 0.5, textTransform: 'uppercase' }}>그룹 이름</div>
            <div style={{ display: 'flex', alignItems: 'baseline', marginTop: 6 }}>
              <span style={{ fontSize: 22, fontWeight: 800, color: T2.text, letterSpacing: -0.6 }}>주말 혼밥 코스</span>
              <span style={{ display: 'inline-block', width: 2, height: 22, background: T2.brand, marginLeft: 2, transform: 'translateY(3px)' }} />
            </div>
            <div style={{ height: 1.5, background: T2.text, marginTop: 8, opacity: 0.85 }} />
          </div>
        </div>

        {/* 아이콘 선택 */}
        <div style={{ marginTop: 22 }}>
          <div style={{ fontSize: 12, fontWeight: 700, color: T2.textMute, letterSpacing: 0.5, textTransform: 'uppercase' }}>아이콘</div>
          <div style={{ marginTop: 12, display: 'flex', gap: 9, flexWrap: 'wrap' }}>
            {emojis.map((e, i) => (
              <div key={i} style={{
                width: 44, height: 44, borderRadius: 12, fontSize: 20,
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                background: i === sel ? T2.brandSoft : '#fff',
                border: `1.5px solid ${i === sel ? T2.brand : T2.border}`,
                cursor: 'pointer',
              }}>{e}</div>
            ))}
          </div>
        </div>

        {/* 메모 */}
        <div style={{ marginTop: 26 }}>
          <div style={{ fontSize: 12, fontWeight: 700, color: T2.textMute, letterSpacing: 0.5, textTransform: 'uppercase' }}>설명 <span style={{ textTransform: 'none', fontWeight: 600 }}>(선택)</span></div>
          <div style={{
            marginTop: 12, padding: '14px 16px', borderRadius: 12, background: '#fff',
            border: `1px solid ${T2.border}`, fontSize: 14, color: T2.textMute, letterSpacing: -0.2,
          }}>혼밥하기 좋은 주말 코스 모음</div>
        </div>

        {/* 공개 설정 */}
        <div style={{
          marginTop: 22, padding: '16px 18px', borderRadius: 12, background: '#fff',
          border: `1px solid ${T2.border}`, display: 'flex', alignItems: 'center', gap: 12,
        }}>
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 14, fontWeight: 800, color: T2.text, letterSpacing: -0.3 }}>나만 보기</div>
            <div style={{ fontSize: 11, color: T2.textMute, marginTop: 2 }}>끄면 메이트에게 공개돼요</div>
          </div>
          <div style={{ width: 46, height: 28, borderRadius: 999, background: T2.brand, position: 'relative', flexShrink: 0 }}>
            <div style={{ position: 'absolute', top: 3, right: 3, width: 22, height: 22, borderRadius: '50%', background: '#fff', boxShadow: '0 1px 3px rgba(0,0,0,0.2)' }} />
          </div>
        </div>

        {/* 만들기 버튼 */}
        <div style={{
          marginTop: 30, padding: '16px 0', borderRadius: 14, background: T2.brand,
          textAlign: 'center', fontSize: 16, fontWeight: 800, color: '#fff', letterSpacing: -0.3,
          boxShadow: '0 6px 18px rgba(255,90,31,0.28)', cursor: 'pointer',
        }}>그룹 만들기</div>
      </div>
    </PhoneShell>
  );
}

Object.assign(window, { NewGroup });
