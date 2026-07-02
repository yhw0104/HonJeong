package com.honjeong.mate.dto;

import jakarta.validation.constraints.NotNull;

public record MateRequestCreateRequest(@NotNull Long toUserId) {
}
