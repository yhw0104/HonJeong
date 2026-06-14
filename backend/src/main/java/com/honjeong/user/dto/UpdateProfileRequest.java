package com.honjeong.user.dto;

import com.honjeong.user.domain.DiningStyle;
import com.honjeong.user.service.UpdateProfileCommand;

import jakarta.validation.constraints.Size;

/**
 * 프로필 부분수정 요청 본문. {@code PATCH /api/users/me}에서 받는다.
 *
 * <p>모든 필드가 선택이다 — 보내지 않은(null) 필드는 수정 대상에서 제외되어 기존 값이 유지된다(PATCH 의미론).
 * 닉네임을 보낼 때는 20자 이하여야 한다({@code @Size(max = 20)}).
 *
 * @param nickname         닉네임(선택, 미전송 시 미변경). 전송 시 20자 이하({@code @Size(max=20)}).
 * @param profileImageUrl  프로필 이미지 URL(선택, 미전송 시 미변경).
 * @param introduction     자기소개 문자열(선택, 미전송 시 미변경).
 * @param region           활동 지역 표시명(선택, 미전송 시 미변경).
 * @param regionLat        활동 지역 위도(선택, 미전송 시 미변경).
 * @param regionLng        활동 지역 경도(선택, 미전송 시 미변경).
 * @param diningStyle      식사 성향({@link DiningStyle} enum, 선택, 미전송 시 미변경).
 * @param allowMealRequest 같이먹기 신청 수신 동의 여부(선택, 미전송 시 미변경).
 */
public record UpdateProfileRequest(
        @Size(max = 20) String nickname,
        String profileImageUrl,
        String introduction,
        String region,
        Double regionLat,
        Double regionLng,
        DiningStyle diningStyle,
        Boolean allowMealRequest) {

    /**
     * 컨트롤러용 요청 DTO를 서비스 계층 입력인 {@link UpdateProfileCommand}로 변환한다.
     *
     * <p>필드를 1:1로 그대로 옮겨 담을 뿐 가공은 없다. 웹 계층 타입(DTO)을 서비스 경계 안으로 직접 들이지 않으려는 분리 목적의
     * 매핑이다 — 컨트롤러는 {@code request.toCommand()}로 변환해
     * {@code userService.updateProfile(userId, command)}에 넘긴다.
     * null 필드는 그대로 전달되며, 서비스에서 null이면 해당 속성을 변경하지 않는다.
     */
    public UpdateProfileCommand toCommand() {
        return new UpdateProfileCommand(nickname, profileImageUrl, introduction,
                region, regionLat, regionLng, diningStyle, allowMealRequest);
    }
}
