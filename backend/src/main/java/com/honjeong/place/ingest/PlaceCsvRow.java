package com.honjeong.place.ingest;

public record PlaceCsvRow(String managementId, String name, String category, String address,
        String roadAddress, String phone, String businessStatusName, String coordX, String coordY) {}
