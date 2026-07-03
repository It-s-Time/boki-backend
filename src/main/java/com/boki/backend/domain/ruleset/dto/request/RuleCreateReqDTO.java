package com.boki.backend.domain.ruleset.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class RuleCreateReqDTO {

    @NotBlank(message = "원칙 내용은 필수입니다.")
    @Size(max = 20, message = "원칙 내용은 20자 이하여야 합니다.")
    private String content;
}
