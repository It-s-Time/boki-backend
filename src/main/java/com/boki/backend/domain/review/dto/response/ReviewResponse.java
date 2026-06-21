package com.boki.backend.domain.review.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ReviewResponse(
        Long reviewId,
        Long tradeId,
        Long memberId,
        Long ruleSetId,
        String content,
        List<ReviewScoreResponse> scores,
        List<String> imageUrls,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
