package com.honjeong.notice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.honjeong.global.config.JpaConfig;
import com.honjeong.notice.domain.Notice;
import com.honjeong.notice.domain.NoticeCategory;
import com.honjeong.support.AbstractPostgresTest;

/** NoticeRepository 슬라이스 테스트(실 Postgres). 핀 우선·게시 최신순·미래 게시 제외를 검증한다. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class NoticeRepositoryTest extends AbstractPostgresTest {

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private TestEntityManager em;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 6, 12, 0);

    @Test
    @DisplayName("목록: 핀 먼저, 그 안에서 게시 최신순, 미래 게시분 제외")
    void list_pinnedFirst_publishedDesc_excludesFuture() {
        Notice pinnedOld = em.persist(Notice.create(NoticeCategory.GENERAL, "고정 공지", "본문", true, NOW.minusDays(10)));
        Notice recent = em.persist(Notice.create(NoticeCategory.UPDATE, "최근 공지", null, false, NOW.minusDays(1)));
        Notice older = em.persist(Notice.create(NoticeCategory.EVENT, "지난 공지", "본문", false, NOW.minusDays(5)));
        em.persist(Notice.create(NoticeCategory.GENERAL, "예약 공지", null, false, NOW.plusDays(1))); // 미래 — 제외
        em.flush();

        List<Notice> got = noticeRepository
                .findByPublishedAtLessThanEqualOrderByPinnedDescPublishedAtDescIdDesc(NOW);

        assertThat(got).extracting(Notice::getId)
                .containsExactly(pinnedOld.getId(), recent.getId(), older.getId());
        assertThat(got.get(0).isPinned()).isTrue();
        assertThat(got.get(0).getCategory()).isEqualTo(NoticeCategory.GENERAL);
        assertThat(got.get(1).getBody()).isNull(); // 본문 없는 공지 허용
    }

    @Test
    @DisplayName("게시 시각이 정확히 now면 포함(경계)")
    void list_includesExactlyNow() {
        Notice atNow = em.persist(Notice.create(NoticeCategory.UPDATE, "지금 게시", null, false, NOW));
        em.flush();

        List<Notice> got = noticeRepository
                .findByPublishedAtLessThanEqualOrderByPinnedDescPublishedAtDescIdDesc(NOW);

        assertThat(got).extracting(Notice::getId).containsExactly(atNow.getId());
    }
}
