package com.boki.backend.domain.review.dto.response;

import com.boki.backend.domain.ruleset.entity.RuleType;

public record WorstRuleResponse(
        Long ruleId,
        String content,
        RuleType ruleType,
        Double complianceRate
) {
}
