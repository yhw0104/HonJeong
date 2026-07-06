package com.honjeong.favorite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 즐겨찾기 그룹 생성 요청 바디 (POST /api/favorite-groups).
 */
public record CreateGroupRequest(
        @NotBlank @Size(max = 20) String name, // 그룹 이름 (필수, 최대 20자)
        @Size(max = 60) String note, // 그룹 메모 (선택, 최대 60자)
        @Size(max = 20) String color) { // 그룹 색상 HEX (선택, 미지정 시 서비스가 기본 #FF5A1F 적용)
}
