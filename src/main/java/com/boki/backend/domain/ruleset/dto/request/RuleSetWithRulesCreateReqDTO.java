package com.boki.backend.domain.ruleset.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;

@Getter
public class RuleSetWithRulesCreateReqDTO {

    @NotBlank(message = "세트 이름은 필수입니다.")
    @Size(max = 50, message = "세트 이름은 50자 이하여야 합니다.")
    private String name;

    private List<@NotBlank(message = "원칙 내용은 필수입니다.") @Size(max = 20, message = "원칙 내용은 20자 이하여야 합니다.") String> buyRules;

    private List<@NotBlank(message = "원칙 내용은 필수입니다.") @Size(max = 20, message = "원칙 내용은 20자 이하여야 합니다.") String> sellRules;
}
