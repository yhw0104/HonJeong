package com.honjeong.favorite.dto;

import jakarta.validation.constraints.Size;

/**
 * 즐겨찾기 그룹 부분 수정 요청 바디 (PATCH /api/favorite-groups/{groupId}) — null 필드는 미변경.
 */
public record UpdateGroupRequest(
        @Size(max = 20) String name, // 새 그룹 이름 (선택, 최대 20자, null이면 미변경)
        @Size(max = 60) String note, // 새 그룹 메모 (선택, 최대 60자, null이면 미변경)
        @Size(max = 20) String color) { // 새 그룹 색상 HEX (선택, 최대 20자, null이면 미변경)
}
