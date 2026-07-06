package com.honjeong.notice.domain;

import java.time.LocalDateTime;
import com.honjeong.global.common.BaseTimeEntity;
import jakarta.persistence.*;

/**
 * 공지사항 한 건. 운영자가 DB 직접 INSERT로 등록하고 앱은 조회만 한다.
 * published_at이 미래면 목록에서 제외된다(예약 게시 겸 초안).
 */
@Entity
@Table(name = "notices")
public class Notice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NoticeCategory category;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(nullable = false)
    private boolean pinned;

    @Column(nullable = false)
    private LocalDateTime publishedAt;

    protected Notice() {
    }

    private Notice(NoticeCategory category, String title, String body, boolean pinned, LocalDateTime publishedAt) {
        this.category = category;
        this.title = title;
        this.body = body;
        this.pinned = pinned;
        this.publishedAt = publishedAt;
    }

    public static Notice create(NoticeCategory category, String title, String body, boolean pinned,
            LocalDateTime publishedAt) {
        return new Notice(category, title, body, pinned, publishedAt);
    }

    public Long getId() {
        return id;
    }

    public NoticeCategory getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public boolean isPinned() {
        return pinned;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }
}
