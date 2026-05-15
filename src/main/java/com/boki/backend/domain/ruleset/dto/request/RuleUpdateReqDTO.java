package com.boki.backend.domain.ruleset.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class RuleUpdateReqDTO {

    @NotBlank(message = "원칙 내용은 필수입니다.")
    private String content;
}
