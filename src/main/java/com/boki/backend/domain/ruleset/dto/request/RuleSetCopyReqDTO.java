package com.boki.backend.domain.ruleset.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class RuleSetCopyReqDTO {

    @NotBlank(message = "세트 이름은 필수입니다.")
    @Size(max = 50, message = "세트 이름은 50자 이하여야 합니다.")
    private String name;
}
