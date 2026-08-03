package com.honjeong.auth.dto;

import java.time.LocalDate;
import java.util.List;

import com.honjeong.auth.service.CompleteProfileCommand;
import com.honjeong.user.domain.DiningStyle;
import com.honjeong.user.domain.Gender;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

/**
 * 온보딩 프로필 완료 요청 본문. {@code POST /api/auth/complete}에서 받는다.
 *
 * <p>닉네임만 필수이고 나머지는 선택 입력이다. {@code gender}/{@code diningStyle}은 enum 타입이라 JSON에서는
 * 문자열({@code MALE}, {@code QUIET} 등)로 보내면 Jackson이 해당 enum 값으로 역직렬화한다.
 *
 * <p><b>문자열 길이 상한은 {@link com.honjeong.user.dto.UpdateProfileRequest}와 같은 값으로 맞춘다</b> —
 * 두 요청이 같은 {@code users} 행을 쓰므로, 한쪽만 느슨하면 프로필 수정으로는 만들 수 없는 값이 가입
 * 경로로 들어와 나중에 DB 제약 위반(=500)으로 터진다.
 *
 * @param nickname        닉네임(필수, {@code @NotBlank}). 20자 이하. 서비스에서 중복 검사를 거친다.
 * @param gender          성별({@link Gender} enum, 선택).
 * @param birthDate       생년월일(선택, 과거 날짜)
 * @param introduction    자기소개 문자열(선택). 150자 이하.
 * @param region          활동 지역 표시명(선택). 100자 이하.
 * @param regionLat       활동 지역 위도(선택).
 * @param regionLng       활동 지역 경도(선택).
 * @param diningStyle     식사 성향({@link DiningStyle} enum, 선택).
 * @param profileImageUrl 프로필 이미지 URL(선택). 500자 이하.
 * @param favoriteFoods   선호 음식 목록(선택). 최대 3개, 각 항목 공백 불가·50자 이내.
 */
public record CompleteProfileRequest(
        @NotBlank @Size(max = 20) String nickname,
        Gender gender,
        @Past LocalDate birthDate,
        @Size(max = 150) String introduction,
        @Size(max = 100) String region,
        Double regionLat,
        Double regionLng,
        DiningStyle diningStyle,
        @Size(max = 500) String profileImageUrl,
        @Size(max = 3) List<@NotBlank @Size(max = 50) String> favoriteFoods) {

    /**
     * 컨트롤러용 요청 DTO를 서비스 계층 입력인 {@link CompleteProfileCommand}로 변환한다.
     *
     * <p>필드를 1:1로 그대로 옮겨 담을 뿐 가공은 없다. 웹 계층 타입(DTO)을 서비스 경계 안으로 직접 들이지 않으려는 분리 목적의
     * 매핑이다 — 컨트롤러는 {@code request.toCommand()}로 변환해 {@code authService.complete(userId, command)}에 넘긴다.
     */
    public CompleteProfileCommand toCommand() {
        return new CompleteProfileCommand(nickname, gender, birthDate, introduction, region, regionLat, regionLng,
                diningStyle, profileImageUrl, favoriteFoods);
    }
}
