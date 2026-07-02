package com.honjeong.mate.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.honjeong.mate.domain.Mate;

public interface MateRepository extends JpaRepository<Mate, Long> {

    boolean existsByUser_IdAndMateUser_Id(Long userId, Long mateUserId);

    @Query("""
            SELECT m FROM Mate m
            JOIN FETCH m.mateUser
            WHERE m.user.id = :userId
            ORDER BY m.createdAt DESC
            """)
    List<Mate> findMatesWithUserByUserId(@Param("userId") Long userId);

    Optional<Mate> findByUser_IdAndMateUser_Id(Long userId, Long mateUserId);
}
