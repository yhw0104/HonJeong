package com.honjeong.notice.dto;

import java.time.LocalDateTime;
import java.util.List;
import com.honjeong.notice.domain.Notice;

/** 공지 목록 응답 — 핀 우선·게시 최신순으로 정렬돼 내려간다. */
public record NoticesResponse(List<Item> notices) {

    public record Item(Long id, String category, String title, String body, boolean pinned,
            LocalDateTime publishedAt) {

        public static Item from(Notice n) {
            return new Item(n.getId(), n.getCategory().name(), n.getTitle(), n.getBody(),
                    n.isPinned(), n.getPublishedAt());
        }
    }

    public static NoticesResponse from(List<Notice> notices) {
        return new NoticesResponse(notices.stream().map(Item::from).toList());
    }
}
