package com.boki.backend.domain.review.dto.response;

import com.boki.backend.domain.ruleset.entity.RuleType;

public record WorstRuleResponse(
        String content,
        RuleType ruleType,
        Double complianceRate
) {
}
