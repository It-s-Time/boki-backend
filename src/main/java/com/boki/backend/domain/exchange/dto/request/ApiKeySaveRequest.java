package com.boki.backend.domain.exchange.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "거래소 API Key 등록/갱신 요청")
public record ApiKeySaveRequest(
        @Schema(description = "거래소 Access Key")
        @NotBlank(message = "Access Key는 필수입니다.")
        @Size(max = 255, message = "Access Key는 255자 이하여야 합니다.")
        String accessKey,

        @Schema(description = "거래소 Secret Key")
        @NotBlank(message = "Secret Key는 필수입니다.")
        @Size(max = 255, message = "Secret Key는 255자 이하여야 합니다.")
        String secretKey
) {
}
