package com.honjeong.chat.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** 매칭 대화 메시지 1건(TEXT는 text, IMAGE는 imageUrl). created_at만 가진다. */
@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Column(name = "sender_user_id", nullable = false)
    private Long senderUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType type;

    @Column(length = 1000)
    private String text;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ChatMessage() {
    }

    private ChatMessage(Conversation conversation, Long senderUserId, MessageType type,
            String text, String imageUrl, LocalDateTime now) {
        this.conversation = conversation;
        this.senderUserId = senderUserId;
        this.type = type;
        this.text = text;
        this.imageUrl = imageUrl;
        this.createdAt = now;
    }

    public static ChatMessage text(Conversation c, Long senderId, String text, LocalDateTime now) {
        return new ChatMessage(c, senderId, MessageType.TEXT, text, null, now);
    }

    public static ChatMessage image(Conversation c, Long senderId, String url, LocalDateTime now) {
        return new ChatMessage(c, senderId, MessageType.IMAGE, null, url, now);
    }

    public Long getId() {
        return id;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public Long getSenderUserId() {
        return senderUserId;
    }

    public MessageType getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
