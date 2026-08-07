export type ConversationStatus = 'ACTIVE' | 'CLOSED';
export type MessageKind = 'TEXT' | 'IMAGE';

export type ConversationSummary = {
  conversationId: number;
  status: ConversationStatus;
  partnerUserId: number;
  partnerNickname: string;
  partnerProfileImageUrl: string | null;
  placeName: string;
  lastMessagePreview: string | null;
  lastMessageAt: string | null;
  unreadCount: number;
  // 상대가 마지막으로 읽은 시각(내 메시지 '읽음' 표시용). 아직 안 읽었으면 null.
  partnerLastReadAt: string | null;
  // 대화방이 열린(매칭 성사) 시각. 메시지가 없어 lastMessageAt이 null일 때 목록에 대신 표시한다.
  createdAt: string;
  // 이 대화의 푸시 알림을 껐는가. 목록 행에 음소거 아이콘으로 표시한다 —
  // 표시가 없으면 사용자는 자기가 껐는지 알 수 없다.
  muted: boolean;
};

export type ChatMessage = {
  id: number;
  senderUserId: number;
  type: MessageKind;
  text: string | null;
  imageUrl: string | null;
  createdAt: string;
};
