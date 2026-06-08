// NotificationSettings.jsx — 알림 설정
// 패턴: 상단 마스터 스위치 + 그룹별(활동/메이트/마케팅) 개별 토글 + 방해 금지 시간

function NotificationSettings() {
  const Toggle = ({ on }) => (
    <div style={{
      width: 46, height: 28, borderRadius: 999, flexShrink: 0, position: 'relative',
      background: on ? T2.brand : 'rgba(0,0,0,0.12)', transition: 'background .15s',
    }}>
      <div style={{
        position: 'absolute', top: 3, left: on ? 21 : 3, width: 22, height: 22, borderRadius: '50%',
        background: '#fff', boxShadow: '0 1px 3px rgba(0,0,0,0.2)', transition: 'left .15s',
      }} />
    </div>
  );

  const Row = ({ title, desc, on, last }) => (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 14, padding: '15px 20px',
      borderBottom: last ? 'none' : `1px solid ${T2.border}`,
    }}>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 15, fontWeight: 600, color: T2.text, letterSpacing: -0.3 }}>{title}</div>
        {desc && <div style={{ fontSize: 12, color: T2.textMute, marginTop: 3, letterSpacing: -0.2 }}>{desc}</div>}
      </div>
      <Toggle on={on} />
    </div>
  );

  const Group = ({ title, children }) => (
    <div style={{ marginTop: 18 }}>
      <div style={{ fontSize: 11, fontWeight: 700, color: T2.textMute, letterSpacing: 0.6, textTransform: 'uppercase', padding: '0 20px 8px' }}>{title}</div>
      <div style={{ background: '#fff', borderTop: `1px solid ${T2.border}`, borderBottom: `1px solid ${T2.border}` }}>{children}</div>
    </div>
  );

  return (
    <PhoneShell bg={T2.bg}>
      <MoreHeader title="알림 설정" />

      <div style={{ position: 'absolute', top: 108, left: 0, right: 0, bottom: 0, overflow: 'auto', paddingBottom: 40 }}>
        {/* 마스터 스위치 */}
        <div style={{ padding: '4px 20px 0' }}>
          <div style={{
            display: 'flex', alignItems: 'center', gap: 14, padding: '18px 18px',
            background: T2.text, borderRadius: 16,
          }}>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 16, fontWeight: 800, color: '#fff', letterSpacing: -0.3 }}>푸시 알림</div>
              <div style={{ fontSize: 12, color: 'rgba(255,255,255,0.6)', marginTop: 3 }}>끄면 아래 모든 알림이 꺼져요</div>
            </div>
            <div style={{
              width: 46, height: 28, borderRadius: 999, flexShrink: 0, position: 'relative', background: T2.brand,
            }}>
              <div style={{ position: 'absolute', top: 3, left: 21, width: 22, height: 22, borderRadius: '50%', background: '#fff', boxShadow: '0 1px 3px rgba(0,0,0,0.25)' }} />
            </div>
          </div>
        </div>

        <Group title="활동">
          <Row title="같이 먹기 신청" desc="새 신청·수락·거절 알림" on={true} />
          <Row title="혼밥 인증 · 리뷰 반응" desc="좋아요·댓글이 달리면" on={true} />
          <Row title="챌린지 · 뱃지" desc="달성 현황과 새 뱃지" on={false} last />
        </Group>

        <Group title="메이트">
          <Row title="메이트 신청" desc="누군가 메이트로 추가하면" on={true} />
          <Row title="메이트 혼밥 시작" desc="내 메이트가 근처에서 혼밥을 시작하면" on={true} last />
        </Group>

        <Group title="마케팅 · 정보">
          <Row title="이벤트 · 혜택" desc="할인·프로모션 소식" on={false} />
          <Row title="공지사항" desc="서비스 주요 변경 안내" on={true} last />
        </Group>

        {/* 방해 금지 시간 */}
        <Group title="방해 금지 시간">
          <Row title="야간 방해 금지" desc="설정한 시간에는 알림을 받지 않아요" on={true} />
          <div style={{ display: 'flex', alignItems: 'center', gap: 14, padding: '15px 20px' }}>
            <span style={{ flex: 1, fontSize: 15, fontWeight: 600, color: T2.text, letterSpacing: -0.3 }}>시간</span>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 14, fontWeight: 700, color: T2.text, fontFeatureSettings: '"tnum"' }}>
              <span style={{ padding: '6px 12px', background: T2.bg, borderRadius: 9, border: `1px solid ${T2.border}` }}>22:00</span>
              <span style={{ color: T2.textMute }}>–</span>
              <span style={{ padding: '6px 12px', background: T2.bg, borderRadius: 9, border: `1px solid ${T2.border}` }}>08:00</span>
            </div>
          </div>
        </Group>

        <div style={{ padding: '18px 20px 0', fontSize: 12, color: T2.textMute, lineHeight: 1.6, letterSpacing: -0.2 }}>
          기기 설정에서 알림이 꺼져 있으면 위 설정과 무관하게 알림이 오지 않을 수 있어요.
        </div>
      </div>
    </PhoneShell>
  );
}

Object.assign(window, { NotificationSettings });
