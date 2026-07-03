package com.boki.backend.domain.ruleset.dto.response;

import com.boki.backend.domain.ruleset.entity.Rule;
import com.boki.backend.domain.ruleset.entity.RuleType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RuleResDTO{

    private Long ruleId;
    private RuleType type;
    private String content;
    private int orderIndex;

    public static RuleResDTO from(Rule rule) {
        return RuleResDTO.builder()
                .ruleId(rule.getId())
                .type(rule.getType())
                .content(rule.getContent())
                .orderIndex(rule.getOrderIndex())
                .build();
    }
}
