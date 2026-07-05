// 신고 사유·상태 표시 문구 — 화면(BlockReport·ReportForm)이 공유하는 순수 로직.

export const REPORT_REASONS = [
  { code: 'INAPPROPRIATE_MESSAGE', label: '부적절한 메시지' },
  { code: 'ABUSE', label: '욕설 / 혐오 표현' },
  { code: 'SPAM', label: '광고 / 스팸' },
  { code: 'FALSE_PROFILE', label: '허위 프로필' },
  { code: 'OTHER', label: '기타' },
] as const;

export type ReportReason = (typeof REPORT_REASONS)[number]['code'];

export function reasonLabel(code: string): string {
  return REPORT_REASONS.find((r) => r.code === code)?.label ?? code;
}

/** 신고 대상 표기 — 유저는 "○○님", 리뷰는 "○○님의 리뷰"(신고 화면·내역 카드 공통). */
export function reportTargetLabel(targetType: string, nickname: string): string {
  return targetType === 'REVIEW' ? `${nickname}님의 리뷰` : `${nickname}님`;
}

/** 처리 상태 라벨. 현재 백엔드는 전부 RECEIVED만 저장하지만 예약 상태도 함께 매핑해 둔다. */
export function reportStatusLabel(status: string): string {
  if (status === 'REVIEWING') return '검토 중';
  if (status === 'RESOLVED') return '처리 완료';
  return '접수됨';
}

/** ISO 일시 → '2026.05.28'. 파싱 불가하면 빈 문자열. */
export function formatDotDate(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '';
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}.${mm}.${dd}`;
}
