package com.honjeong.favorite.dto;

import jakarta.validation.constraints.Size;

public record UpdateGroupRequest(
        @Size(max = 20) String name,
        @Size(max = 60) String note,
        @Size(max = 20) String color) {
}
