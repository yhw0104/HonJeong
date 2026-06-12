package com.honjeong.auth.service;

import com.honjeong.user.domain.DiningStyle;
import com.honjeong.user.domain.Gender;

/**
 * 온보딩 마지막 단계(프로필 완료)의 입력값을 한데 묶은 불변 값 객체(record). {@link AuthService#complete}에
 * 전달되어 회원 프로필을 채우는 데 쓰인다. 여러 인자를 개별로 넘기는 대신 하나의 명령(command) 객체로
 * 묶어 시그니처를 간결하게 유지한다.
 *
 * @param nickname        닉네임(중복 불가 — 가입 시 유일성 검사 대상)
 * @param gender          성별
 * @param ageGroup        연령대(예: "20대")
 * @param introduction    자기소개 문구
 * @param region          활동 지역명
 * @param regionLat       지역 위도(좌표)
 * @param regionLng       지역 경도(좌표)
 * @param diningStyle     식사 스타일(취향)
 * @param profileImageUrl 프로필 이미지 URL
 */
public record CompleteProfileCommand(
        String nickname,
        Gender gender,
        String ageGroup,
        String introduction,
        String region,
        Double regionLat,
        Double regionLng,
        DiningStyle diningStyle,
        String profileImageUrl) {
}
