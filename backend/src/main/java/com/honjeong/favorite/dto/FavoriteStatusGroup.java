package com.honjeong.favorite.dto;

/**
 * 즐겨찾기 상태 조회에서 그룹 1개의 담김 여부를 나타내는 응답 요소 (FavoriteStatusResponse.groups).
 *
 * @param groupId  그룹 ID
 * @param name     그룹 이름
 * @param color    그룹 색상 HEX
 * @param contains 조회한 장소가 이 그룹에 담겨 있는지 여부
 */
public record FavoriteStatusGroup(Long groupId, String name, String color, boolean contains) {
}
