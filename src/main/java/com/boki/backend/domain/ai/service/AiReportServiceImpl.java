package com.boki.backend.domain.ai.service;

import com.boki.backend.domain.ai.client.OpenAiClient;
import com.boki.backend.domain.ai.dto.AiContentDTO;
import com.boki.backend.domain.ai.dto.response.AiReportResDTO;
import com.boki.backend.domain.ai.entity.AiReport;
import com.boki.backend.domain.ai.entity.Grade;
import com.boki.backend.domain.ai.exception.AiReportErrorCode;
import com.boki.backend.domain.ai.repository.AiReportRepository;
import com.boki.backend.domain.ruleset.entity.Rule;
import com.boki.backend.domain.ruleset.repository.RuleRepository;
import com.boki.backend.domain.trade.entity.Trade;
import com.boki.backend.domain.trade.repository.TradeRepository;
import com.boki.backend.global.apiPayload.exception.GeneralException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiReportServiceImpl implements AiReportService {

    private static final String SYSTEM_PROMPT =
            """
            당신은 암호화폐 트레이딩 멘토입니다. 트레이더의 거래 정보와 매매 원칙을 분석하여 피드백을 제공합니다.
            반드시 아래 JSON 형식으로만 응답하세요:
            {
              "goodPoints": ["잘한 점 1", "잘한 점 2"],
              "badPoints": ["아쉬운 점 1", "아쉬운 점 2"],
              "recommendedRule": {
                "type": "BUY 또는 SELL",
                "content": "추천 매매 원칙 내용"
              }
            }
            모든 응답은 한국어로 작성하세요.
            """;

    private final TradeRepository tradeRepository;
    private final RuleRepository ruleRepository;
    private final AiReportRepository aiReportRepository;
    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public AiReportResDTO generateReport(Long memberId, Long tradeId) {
        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new GeneralException(AiReportErrorCode.TRADE_NOT_FOUND));

        if (!trade.getMemberId().equals(memberId)) {
            throw new GeneralException(AiReportErrorCode.TRADE_ACCESS_DENIED);
        }

        if (aiReportRepository.existsByTradeId(tradeId)) {
            throw new GeneralException(AiReportErrorCode.AI_REPORT_ALREADY_EXISTS);
        }

        AiReport report = AiReport.builder()
                .tradeId(tradeId)
                .memberId(memberId)
                .build();
        aiReportRepository.save(report);

        try {
            String userPrompt = buildUserPrompt(trade);
            String rawContent = openAiClient.requestCompletion(SYSTEM_PROMPT, userPrompt);

            // TODO: 복기 점수 데이터 연동 후 compliance_rate 및 grade 계산 추가
            // ex) Double complianceRate = reviewScoreService.calcComplianceRate(tradeId);
            // ex) Grade grade = Grade.from(complianceRate);
            Double complianceRate = null;
            Grade grade = null;

            report.complete(rawContent, complianceRate, grade);
            return toResDTO(report, rawContent);
        } catch (GeneralException e) {
            report.fail();
            throw e;
        }
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

    private String buildUserPrompt(Trade trade) {
        StringBuilder sb = new StringBuilder();
        sb.append("[거래 정보]\n");
        sb.append("- 거래 유형: ").append(trade.getTradeType()).append("\n");
        sb.append("- 종목: ").append(trade.getCoinType()).append("\n");
        sb.append("- 가격: ").append(trade.getPrice()).append("\n");
        sb.append("- 수량: ").append(trade.getQuantity()).append("\n");
        sb.append("- 거래 일시: ").append(trade.getTradedAt()).append("\n\n");

        if (trade.getRuleSetId() != null) {
            List<Rule> rules = ruleRepository.findByRuleSetIdAndIsActiveTrueOrderByOrderIndexAsc(trade.getRuleSetId());
            if (!rules.isEmpty()) {
                sb.append("[적용된 매매 원칙]\n");
                rules.forEach(rule ->
                        sb.append("- [").append(rule.getType()).append("] ").append(rule.getContent()).append("\n")
                );
                sb.append("\n");
            }
        }

        // TODO: 복기 점수 데이터 연동 후 아래 섹션 추가
        // sb.append("[복기 점수]\n");
        // reviewScores.forEach(score -> sb.append("- ").append(score.getRuleContent())
        //         .append(": ").append(score.getScore()).append("/5\n"));

        return sb.toString();
    }

    private AiReportResDTO toResDTO(AiReport report, String rawContent) {
        if (rawContent == null) {
            return new AiReportResDTO(
                    report.getAiReportId(), report.getTradeId(), report.getStatus(),
                    report.getGrade(), report.getComplianceRate(), null, null, null
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
                    content.goodPoints(), content.badPoints(), recommendedRule
            );
        } catch (Exception e) {
            throw new GeneralException(AiReportErrorCode.AI_RESPONSE_PARSE_FAILED);
        }
    }
}
