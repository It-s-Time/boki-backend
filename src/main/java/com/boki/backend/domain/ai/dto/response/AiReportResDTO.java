package com.boki.backend.domain.ai.dto.response;

import com.boki.backend.domain.ai.entity.Grade;
import com.boki.backend.domain.ai.entity.ReportStatus;
import java.util.List;

public record AiReportResDTO(
        Long aiReportId,
        Long tradeId,
        ReportStatus status,
        Grade grade,
        Double complianceRate,
        List<String> hashtags,
        List<String> goodPoints,
        List<String> badPoints,
        RecommendedRuleResDTO recommendedRule
) {
    public record RecommendedRuleResDTO(
            String type,
            String content
    ) {}
}
