package com.honjeong.user.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.honjeong.user.domain.UserFoodPreference;
import com.honjeong.user.repository.UserFoodPreferenceRepository;

/**
 * 1. 기능: 회원 선호 음식(최대 3개)의 upsert·조회 — "사용자당 1행" 의미론 담당
 * 2. 사용 Controller: 없음 — AuthService, UserService(서비스에서만 사용)
 *
 * <p>[기존 주석] 선호 음식 upsert·조회 서비스. 가입(AuthService.complete)·수정(UserService.updateProfile)이 공유한다.
 * 음식↔컬럼 변환은 엔티티가 맡고, 여기서는 "사용자당 1행" upsert 의미론만 책임진다.
 */
@Service
public class UserFoodPreferenceService {

    private final UserFoodPreferenceRepository repository;

    public UserFoodPreferenceService(UserFoodPreferenceRepository repository) {
        this.repository = repository;
    }

    /**
     * 기능: 회원의 선호 음식 목록을 통째로 교체한다(upsert; null이면 미변경)
     * Request: userId — 회원 ID, foods — 새 음식 목록(null=미변경, 빈 목록=비움, 값 있으면 생성/교체)
     * Response: List&lt;String&gt; — 반영된 최종 음식 목록(0~3개)
     *
     * <p>[기존 주석] 회원의 선호 음식을 통째로 교체한다(upsert).
     *
     * @param foods 새 음식 목록. {@code null}이면 변경하지 않고 현재값을 그대로 돌려준다(PATCH 미전송 의미).
     *              빈 목록이면 기존 행을 비우고(없으면 아무 것도 하지 않음), 값이 있으면 행을 만들거나 교체한다.
     * @return 반영된 최종 음식 목록(0~3개)
     */
    @Transactional
    public List<String> replaceFoods(Long userId, List<String> foods) {
        UserFoodPreference pref = repository.findByUserId(userId).orElse(null);
        if (foods == null) {
            return pref == null ? List.of() : pref.toFoods(); // 미변경
        }
        if (pref == null) {
            if (foods.isEmpty()) {
                return List.of(); // 비울 행도 없음
            }
            pref = repository.save(UserFoodPreference.of(userId, foods));
        } else {
            pref.updateFoods(foods); // dirty checking → 커밋 시 UPDATE
        }
        return pref.toFoods();
    }

    /**
     * 기능: 회원의 선호 음식 목록을 조회한다
     * Request: userId — 회원 ID
     * Response: List&lt;String&gt; — 선호 음식 목록(0~3개, 없으면 빈 목록)
     *
     * <p>[기존 주석] 회원의 선호 음식을 조회한다(없으면 빈 목록).
     */
    @Transactional(readOnly = true)
    public List<String> getFoods(Long userId) {
        return repository.findByUserId(userId)
                .map(UserFoodPreference::toFoods)
                .orElse(List.of());
    }
}
