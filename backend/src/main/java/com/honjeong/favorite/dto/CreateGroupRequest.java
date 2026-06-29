package com.honjeong.favorite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateGroupRequest(
        @NotBlank @Size(max = 20) String name,
        @Size(max = 60) String note,
        @Size(max = 20) String color) {
}
