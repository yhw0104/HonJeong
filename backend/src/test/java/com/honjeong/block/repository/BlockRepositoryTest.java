package com.honjeong.block.repository;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import com.honjeong.block.domain.Block;
import com.honjeong.global.config.JpaConfig;
import com.honjeong.support.AbstractPostgresTest;
import com.honjeong.user.domain.User;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class BlockRepositoryTest extends AbstractPostgresTest {

    @Autowired private BlockRepository blockRepository;
    @Autowired private TestEntityManager em;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 2, 12, 0);

    private User userA;
    private User userB;
    private User userC;

    private User persistUser(String phone, String nickname) {
        User user = User.pending(phone, null);
        user.completeProfile(nickname, null, null, null, null, null, null, null, null);
        return em.persist(user);
    }

    @BeforeEach
    void setUp() {
        userA = persistUser("01000000001", "유저A");
        userB = persistUser("01000000002", "유저B");
        userC = persistUser("01000000003", "유저C");
    }

    @Test
    @DisplayName("같은 쌍 중복 차단은 uq_blocks_pair 위반")
    void duplicatePair_violatesUnique() {
        blockRepository.saveAndFlush(Block.create(userA, userB, NOW));
        assertThatThrownBy(() ->
                blockRepository.saveAndFlush(Block.create(userA, userB, NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("existsBlockBetween은 방향 무관 true")
    void existsBlockBetween_bidirectional() {
        blockRepository.saveAndFlush(Block.create(userA, userB, NOW));
        assertThat(blockRepository.existsBlockBetween(userA.getId(), userB.getId())).isTrue();
        assertThat(blockRepository.existsBlockBetween(userB.getId(), userA.getId())).isTrue();
        assertThat(blockRepository.existsBlockBetween(userA.getId(), userC.getId())).isFalse();
    }

    @Test
    @DisplayName("findCounterpartIds는 blocker/blocked 양쪽 상대의 합집합")
    void findCounterpartIds_union() {
        blockRepository.saveAndFlush(Block.create(userA, userB, NOW)); // A가 차단
        blockRepository.saveAndFlush(Block.create(userC, userA, NOW)); // A가 차단당함
        assertThat(blockRepository.findCounterpartIds(userA.getId()))
                .containsExactlyInAnyOrder(userB.getId(), userC.getId());
    }

    @Test
    @DisplayName("findExclusionIds는 차단 없으면 센티널 [-1]")
    void findExclusionIds_sentinelWhenEmpty() {
        assertThat(blockRepository.findExclusionIds(userA.getId())).containsExactly(-1L);
    }

    @Test
    @DisplayName("findAllWithBlockedByBlocker는 내가 차단한 것만 최신순")
    void findAllWithBlockedByBlocker() {
        blockRepository.saveAndFlush(Block.create(userA, userB, NOW));
        blockRepository.saveAndFlush(Block.create(userC, userA, NOW)); // 내가 당한 건 제외
        List<Block> result = blockRepository.findAllWithBlockedByBlocker(userA.getId());
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBlocked().getId()).isEqualTo(userB.getId());
    }
}
