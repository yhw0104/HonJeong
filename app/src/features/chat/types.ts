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
};

export type ChatMessage = {
  id: number;
  senderUserId: number;
  type: MessageKind;
  text: string | null;
  imageUrl: string | null;
  createdAt: string;
};
