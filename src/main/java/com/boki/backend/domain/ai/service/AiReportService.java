package com.boki.backend.domain.ai.service;

import com.boki.backend.domain.ai.dto.response.AiReportResDTO;

public interface AiReportService {

    AiReportResDTO generateReport(Long memberId, Long tradeId);

    AiReportResDTO getReport(Long memberId, Long tradeId);
}
