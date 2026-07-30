package com.honjeong.notice.dto;

import java.time.LocalDateTime;
import java.util.List;
import com.honjeong.notice.domain.Notice;

/**
 * 공지 목록 API 응답 DTO.
 *
 * <p>공지 목록 응답 — 핀 우선·게시 최신순으로 정렬돼 내려간다.
 *
 * @param notices 공지 목록(핀 우선·게시 최신순)
 */
public record NoticesResponse(List<Item> notices) {

    /**
     * 공지 한 건의 응답 항목.
     *
     * @param id 공지 ID
     * @param category 공지 카테고리 문자열(UPDATE/EVENT/GENERAL)
     * @param title 공지 제목
     * @param body 공지 본문
     * @param pinned 상단 고정 여부
     * @param publishedAt 게시 시각(KST)
     */
    public record Item(Long id, String category, String title, String body, boolean pinned,
            LocalDateTime publishedAt) {

        /** 기능: Notice 엔티티 한 건을 응답 항목으로 변환 */
        public static Item from(Notice n) {
            return new Item(n.getId(), n.getCategory().name(), n.getTitle(), n.getBody(),
                    n.isPinned(), n.getPublishedAt());
        }
    }

    /** 기능: Notice 엔티티 목록을 응답 DTO로 변환(순서 유지) */
    public static NoticesResponse from(List<Notice> notices) {
        return new NoticesResponse(notices.stream().map(Item::from).toList());
    }
}
