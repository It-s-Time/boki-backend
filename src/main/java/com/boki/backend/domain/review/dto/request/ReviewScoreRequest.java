package com.boki.backend.domain.review.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReviewScoreRequest(
        @NotNull(message = "원칙 ID는 필수입니다.")
        Long ruleId,

        @NotNull(message = "원칙 점수는 필수입니다.")
        @Min(value = 1, message = "원칙 점수는 1점 이상이어야 합니다.")
        @Max(value = 5, message = "원칙 점수는 5점 이하여야 합니다.")
        Integer score
) {
}
