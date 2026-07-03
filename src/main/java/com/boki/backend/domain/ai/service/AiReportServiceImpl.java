package com.boki.backend.domain.ai.service;

import com.boki.backend.domain.ai.dto.AiContentDTO;
import com.boki.backend.domain.ai.dto.response.AiReportResDTO;
import com.boki.backend.domain.ai.entity.AiReport;
import com.boki.backend.domain.ai.entity.ReportStatus;
import com.boki.backend.domain.ai.exception.AiReportErrorCode;
import com.boki.backend.domain.ai.repository.AiReportRepository;
import com.boki.backend.domain.trade.entity.Trade;
import com.boki.backend.domain.trade.repository.TradeRepository;
import com.boki.backend.global.apiPayload.exception.GeneralException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class AiReportServiceImpl implements AiReportService {

    private final TradeRepository tradeRepository;
    private final AiReportRepository aiReportRepository;
    private final AiReportAsyncService aiReportAsyncService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public AiReportResDTO generateReport(Long memberId, Long tradeId) {
        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new GeneralException(AiReportErrorCode.TRADE_NOT_FOUND));

        if (!trade.getMemberId().equals(memberId)) {
            throw new GeneralException(AiReportErrorCode.TRADE_ACCESS_DENIED);
        }

        if (aiReportRepository.existsByTradeIdAndStatusIn(
                tradeId, List.of(ReportStatus.PENDING, ReportStatus.COMPLETED))) {
            throw new GeneralException(AiReportErrorCode.AI_REPORT_ALREADY_EXISTS);
        }

        AiReport report = aiReportRepository.findByTradeId(tradeId)
                .map(existing -> { existing.reset(); return existing; })
                .orElseGet(() -> AiReport.builder().tradeId(tradeId).memberId(memberId).build());
        aiReportRepository.save(report);

        // 트랜잭션 커밋 완료 후 비동기 실행
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                aiReportAsyncService.processReport(tradeId);
            }
        });

        return toResDTO(report, null); // PENDING 상태 즉시 반환
    }

    @Override
    @Transactional(readOnly = true)
    public AiReportResDTO getReport(Long memberId, Long tradeId) {
        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new GeneralException(AiReportErrorCode.TRADE_NOT_FOUND));

        if (!trade.getMemberId().equals(memberId)) {
            throw new GeneralException(AiReportErrorCode.TRADE_ACCESS_DENIED);
        }

        AiReport report = aiReportRepository.findByTradeId(tradeId)
                .orElseThrow(() -> new GeneralException(AiReportErrorCode.AI_REPORT_NOT_FOUND));

        return toResDTO(report, report.getContent());
    }

    private AiReportResDTO toResDTO(AiReport report, String rawContent) {
        if (rawContent == null) {
            return new AiReportResDTO(
                    report.getAiReportId(), report.getTradeId(), report.getStatus(),
                    report.getGrade(), report.getComplianceRate(), null, null, null, null
            );
        }

        try {
            AiContentDTO content = objectMapper.readValue(rawContent, AiContentDTO.class);
            AiReportResDTO.RecommendedRuleResDTO recommendedRule = content.recommendedRule() != null
                    ? new AiReportResDTO.RecommendedRuleResDTO(
                            content.recommendedRule().type(),
                            content.recommendedRule().content())
                    : null;

            return new AiReportResDTO(
                    report.getAiReportId(), report.getTradeId(), report.getStatus(),
                    report.getGrade(), report.getComplianceRate(),
                    content.hashtags(), content.goodPoints(), content.badPoints(), recommendedRule
            );
        } catch (Exception e) {
            throw new GeneralException(AiReportErrorCode.AI_RESPONSE_PARSE_FAILED);
        }
    }
}
