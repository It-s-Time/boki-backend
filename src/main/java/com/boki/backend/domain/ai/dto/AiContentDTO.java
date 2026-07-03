package com.boki.backend.domain.ai.dto;

import java.util.List;

public record AiContentDTO(
        List<String> hashtags,
        List<String> goodPoints,
        List<String> badPoints,
        RecommendedRuleDTO recommendedRule
) {
    public record RecommendedRuleDTO(
            String type,
            String content
    ) {}
}
