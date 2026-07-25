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
};

export type ChatMessage = {
  id: number;
  senderUserId: number;
  type: MessageKind;
  text: string | null;
  imageUrl: string | null;
  createdAt: string;
};
