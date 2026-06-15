package com.honjeong.checkin.dto;

import com.honjeong.place.service.PlaceUpsertCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 체크인 시작 요청. 검색 결과에서 받은 가게 정보를 그대로 담아 보낸다. external_id로 places upsert가 일어난다.
 * 좌표는 누락을 감지하려고 {@code Double} + {@code @NotNull}로 받는다(엔티티/커맨드는 primitive double).
 *
 * @param externalId 카카오 place id(캐싱 키, 필수)
 * @param name       가게명(필수)
 * @param address    주소(선택)
 * @param latitude   위도(필수)
 * @param longitude  경도(필수)
 * @param category   카테고리(선택)
 */
public record CheckInRequest(
        @NotBlank String externalId,
        @NotBlank String name,
        String address,
        @NotNull Double latitude,
        @NotNull Double longitude,
        String category) {

    /** PlaceService.findOrCreateByExternalId 입력으로 변환한다. */
    public PlaceUpsertCommand toUpsertCommand() {
        return new PlaceUpsertCommand(externalId, name, address, latitude, longitude, category);
    }
}
