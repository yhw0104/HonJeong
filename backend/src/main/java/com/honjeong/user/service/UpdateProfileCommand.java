package com.honjeong.user.service;

import java.util.List;

import com.honjeong.user.domain.DiningStyle;

/**
 * 프로필 부분수정 서비스 입력. 웹 DTO를 서비스 경계 안으로 들이지 않으려는 분리용.
 * 모든 필드 선택적 — null이면 기존 값 유지, null이 아니면 해당 필드 교체.
 *
 * @param nickname         새 닉네임(선택)
 * @param profileImageUrl  새 프로필 이미지 URL(선택)
 * @param introduction     새 한 줄 소개(선택)
 * @param region           새 활동 지역명(선택)
 * @param regionLat        새 지역 위도(선택)
 * @param regionLng        새 지역 경도(선택)
 * @param diningStyle      새 식사 성향(선택)
 * @param allowMealRequest 같이먹기 수신 허용 토글(선택)
 * @param favoriteFoods    선호 음식 목록(선택). null=미변경, 목록이면 통째로 교체(빈 목록=비움)
 */
public record UpdateProfileCommand(
        String nickname,
        String profileImageUrl,
        String introduction,
        String region,
        Double regionLat,
        Double regionLng,
        DiningStyle diningStyle,
        Boolean allowMealRequest,
        List<String> favoriteFoods) {
}
