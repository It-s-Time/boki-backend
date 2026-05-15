package com.boki.backend.domain.ruleset.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class RuleSetUpdateReqDTO {

    @NotBlank(message = "세트 이름은 필수입니다.")
    private String name;
}
