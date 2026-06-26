package com.boki.backend.domain.review.dto.response;

public record ReviewScoreResponse(
        Long ruleId,
        String ruleContent,
        Integer score
) {
}
