package com.boki.backend.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginCodeExchangeRequest(
        @NotBlank(message = "loginCode는 필수입니다.")
        String loginCode
) {
}
