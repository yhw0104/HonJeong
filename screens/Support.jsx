// Support.jsx — 고객센터 · 문의
// 패턴: 검색바 + 빠른 문의 채널(1:1 채팅·이메일) + 자주 묻는 질문 아코디언 + 운영시간

function Support() {
  const faqs = [
    { q: '같이 먹기 신청은 어떻게 보내나요?', open: true,
      a: '식당 상세 화면 하단의 "같이 먹기 신청" 버튼을 누른 뒤, 지금 혼밥 중인 사람을 선택하고 인사 한마디를 보내면 돼요.' },
    { q: '혼밥 인증은 어떻게 하나요?', open: false },
    { q: '메이트를 차단하면 어떻게 되나요?', open: false },
    { q: '동네(위치)는 어떻게 바꾸나요?', open: false },
    { q: '계정을 삭제하고 싶어요', open: false },
  ];

  return (
    <PhoneShell bg={T2.bg}>
      <MoreHeader title="고객센터" />

      <div style={{ position: 'absolute', top: 108, left: 0, right: 0, bottom: 0, overflow: 'auto', padding: '4px 20px 40px' }}>
        {/* 검색 */}
        <div style={{
          display: 'flex', alignItems: 'center', gap: 9, padding: '13px 15px',
          background: '#fff', borderRadius: 12, border: `1px solid ${T2.border}`,
        }}>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
            <circle cx="11" cy="11" r="7" stroke={T2.textMute} strokeWidth="1.8"/>
            <path d="M20 20l-3.2-3.2" stroke={T2.textMute} strokeWidth="1.8" strokeLinecap="round"/>
          </svg>
          <span style={{ fontSize: 14, color: T2.textMute, letterSpacing: -0.2 }}>무엇을 도와드릴까요?</span>
        </div>

        {/* 빠른 문의 채널 */}
        <div style={{ display: 'flex', gap: 10, marginTop: 14 }}>
          <div style={{
            flex: 1, padding: '18px 14px', background: T2.text, borderRadius: 16, cursor: 'pointer',
          }}>
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
              <path d="M4 5a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H9l-4 4v-4H6a2 2 0 0 1-2-2V5z" stroke="#fff" strokeWidth="1.7" strokeLinejoin="round"/>
            </svg>
            <div style={{ fontSize: 15, fontWeight: 800, color: '#fff', marginTop: 12, letterSpacing: -0.3 }}>1:1 문의</div>
            <div style={{ fontSize: 11, color: 'rgba(255,255,255,0.6)', marginTop: 3 }}>보통 1시간 내 답변</div>
          </div>
          <div style={{
            flex: 1, padding: '18px 14px', background: '#fff', borderRadius: 16, border: `1px solid ${T2.border}`, cursor: 'pointer',
          }}>
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
              <rect x="3" y="5" width="18" height="14" rx="2" stroke={T2.text} strokeWidth="1.7"/>
              <path d="M4 7l8 6 8-6" stroke={T2.text} strokeWidth="1.7" strokeLinejoin="round"/>
            </svg>
            <div style={{ fontSize: 15, fontWeight: 800, color: T2.text, marginTop: 12, letterSpacing: -0.3 }}>이메일 문의</div>
            <div style={{ fontSize: 11, color: T2.textMute, marginTop: 3 }}>help@honjeong.kr</div>
          </div>
        </div>

        {/* 자주 묻는 질문 */}
        <div style={{ fontSize: 11, fontWeight: 700, color: T2.textMute, letterSpacing: 0.6, textTransform: 'uppercase', margin: '28px 0 10px' }}>
          자주 묻는 질문
        </div>
        <div style={{ background: '#fff', borderRadius: 14, border: `1px solid ${T2.border}`, overflow: 'hidden' }}>
          {faqs.map((f, i) => (
            <div key={i} style={{ borderBottom: i < faqs.length - 1 ? `1px solid ${T2.border}` : 'none' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '15px 16px', cursor: 'pointer' }}>
                <span style={{ fontSize: 15, fontWeight: 700, color: T2.brand, flexShrink: 0 }}>Q</span>
                <span style={{ flex: 1, fontSize: 14, fontWeight: 600, color: T2.text, letterSpacing: -0.3, lineHeight: 1.4 }}>{f.q}</span>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" style={{ flexShrink: 0, transform: f.open ? 'rotate(180deg)' : 'none' }}>
                  <path d="M6 9l6 6 6-6" stroke={T2.textMute} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                </svg>
              </div>
              {f.open && f.a && (
                <div style={{ display: 'flex', gap: 10, padding: '0 16px 16px' }}>
                  <span style={{ fontSize: 15, fontWeight: 700, color: T2.textMute, flexShrink: 0 }}>A</span>
                  <span style={{ flex: 1, fontSize: 13, color: T2.textSub, lineHeight: 1.65, letterSpacing: -0.3 }}>{f.a}</span>
                </div>
              )}
            </div>
          ))}
        </div>

        {/* 운영 정보 */}
        <div style={{
          marginTop: 18, padding: 16, background: T2.bg, borderRadius: 12, border: `1px solid ${T2.border}`,
        }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13, letterSpacing: -0.2 }}>
            <span style={{ color: T2.textMute }}>운영 시간</span>
            <span style={{ color: T2.text, fontWeight: 600, fontFeatureSettings: '"tnum"' }}>평일 10:00 – 18:00</span>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13, letterSpacing: -0.2, marginTop: 9 }}>
            <span style={{ color: T2.textMute }}>점심시간</span>
            <span style={{ color: T2.text, fontWeight: 600, fontFeatureSettings: '"tnum"' }}>13:00 – 14:00</span>
          </div>
        </div>
      </div>
    </PhoneShell>
  );
}

Object.assign(window, { Support });
