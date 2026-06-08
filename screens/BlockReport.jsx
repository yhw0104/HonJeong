// BlockReport.jsx — 차단 / 신고 관리
// 패턴: 상단 탭(차단 목록 / 신고 내역) + 차단 유저 리스트(해제 버튼) + 신고 처리 상태

function BlockReport() {
  const [tab, setTab] = React.useState('block');

  const blocked = [
    { name: '소란한식객', emo: '🍔', date: '2026.05.28' },
    { name: '늦참러', emo: '🥡', date: '2026.05.12' },
  ];
  const reports = [
    { target: '익명 메이트', reason: '부적절한 메시지', date: '2026.05.30', status: '처리 완료' },
    { target: '게시물 리뷰', reason: '광고 · 스팸', date: '2026.05.20', status: '검토 중' },
  ];

  return (
    <PhoneShell bg={T2.bg}>
      <MoreHeader title="차단 / 신고 관리" />

      {/* 탭 */}
      <div style={{ position: 'absolute', top: 104, left: 0, right: 0, zIndex: 9, background: T2.bg, padding: '0 20px' }}>
        <div style={{ display: 'flex', gap: 24 }}>
          {[
            { key: 'block', label: '차단 목록', count: blocked.length },
            { key: 'report', label: '신고 내역', count: reports.length },
          ].map((s) => {
            const on = tab === s.key;
            return (
              <div key={s.key} onClick={() => setTab(s.key)} style={{
                position: 'relative', paddingBottom: 12, cursor: 'pointer',
                display: 'flex', alignItems: 'center', gap: 6,
              }}>
                <span style={{ fontSize: 15, fontWeight: on ? 800 : 600, color: on ? T2.text : T2.textMute, letterSpacing: -0.3 }}>{s.label}</span>
                <span style={{ fontSize: 12, fontWeight: 700, color: on ? T2.brand : T2.textMute, fontFeatureSettings: '"tnum"' }}>{s.count}</span>
                {on && <div style={{ position: 'absolute', left: 0, right: 0, bottom: 0, height: 2, background: T2.brand }} />}
              </div>
            );
          })}
        </div>
        <div style={{ height: 1, background: T2.border, marginTop: -1 }} />
      </div>

      <div style={{ position: 'absolute', top: 150, left: 0, right: 0, bottom: 0, overflow: 'auto', padding: '16px 20px 40px' }}>
        {tab === 'block' ? (
          <React.Fragment>
            <div style={{ fontSize: 12, color: T2.textMute, lineHeight: 1.6, letterSpacing: -0.2, marginBottom: 14 }}>
              차단한 메이트는 서로의 프로필·혼밥 현황을 볼 수 없고, 같이 먹기 신청도 보낼 수 없어요.
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
              {blocked.map((b, i) => (
                <div key={i} style={{
                  display: 'flex', alignItems: 'center', gap: 13, padding: 14,
                  background: '#fff', borderRadius: 14, border: `1px solid ${T2.border}`,
                }}>
                  <div style={{
                    width: 44, height: 44, borderRadius: '50%', flexShrink: 0,
                    background: T2.bg, border: `1px solid ${T2.border}`,
                    display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 21,
                    filter: 'grayscale(1)', opacity: 0.7,
                  }}>{b.emo}</div>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: 15, fontWeight: 700, color: T2.text, letterSpacing: -0.3 }}>{b.name}</div>
                    <div style={{ fontSize: 12, color: T2.textMute, marginTop: 3, fontFeatureSettings: '"tnum"' }}>{b.date} 차단</div>
                  </div>
                  <div style={{
                    flexShrink: 0, padding: '8px 14px', borderRadius: 9, cursor: 'pointer',
                    fontSize: 13, fontWeight: 700, color: T2.text, letterSpacing: -0.2,
                    background: '#fff', border: `1px solid ${T2.borderStrong}`,
                  }}>차단 해제</div>
                </div>
              ))}
            </div>
          </React.Fragment>
        ) : (
          <React.Fragment>
            <div style={{ fontSize: 12, color: T2.textMute, lineHeight: 1.6, letterSpacing: -0.2, marginBottom: 14 }}>
              신고는 운영팀이 확인 후 조치하며, 처리 결과를 알림으로 알려드려요.
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
              {reports.map((r, i) => {
                const done = r.status === '처리 완료';
                return (
                  <div key={i} style={{
                    padding: 16, background: '#fff', borderRadius: 14, border: `1px solid ${T2.border}`,
                  }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      <span style={{ flex: 1, fontSize: 15, fontWeight: 700, color: T2.text, letterSpacing: -0.3 }}>{r.target}</span>
                      <span style={{
                        fontSize: 11, fontWeight: 700, letterSpacing: -0.2, padding: '4px 9px', borderRadius: 999,
                        color: done ? '#1B8049' : T2.brand,
                        background: done ? 'rgba(34,166,90,0.1)' : T2.brandSoft,
                      }}>{r.status}</span>
                    </div>
                    <div style={{ display: 'flex', gap: 8, marginTop: 10, alignItems: 'center' }}>
                      <span style={{ fontSize: 13, color: T2.textSub, letterSpacing: -0.2 }}>사유 · {r.reason}</span>
                      <span style={{ color: T2.textMute }}>·</span>
                      <span style={{ fontSize: 13, color: T2.textMute, fontFeatureSettings: '"tnum"', letterSpacing: -0.2 }}>{r.date}</span>
                    </div>
                  </div>
                );
              })}
            </div>
          </React.Fragment>
        )}
      </div>
    </PhoneShell>
  );
}

Object.assign(window, { BlockReport });
