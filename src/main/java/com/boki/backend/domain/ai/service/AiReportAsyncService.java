package com.boki.backend.domain.ai.service;

import com.boki.backend.domain.ai.client.OpenAiClient;
import com.boki.backend.domain.ai.entity.AiReport;
import com.boki.backend.domain.ai.entity.Grade;
import com.boki.backend.domain.ai.repository.AiReportRepository;
import com.boki.backend.domain.review.entity.ReviewScore;
import com.boki.backend.domain.review.entity.TradeReview;
import com.boki.backend.domain.review.repository.ReviewScoreRepository;
import com.boki.backend.domain.review.repository.TradeReviewRepository;
import com.boki.backend.domain.ruleset.entity.Rule;
import com.boki.backend.domain.ruleset.repository.RuleRepository;
import com.boki.backend.domain.trade.entity.Trade;
import com.boki.backend.domain.trade.repository.TradeRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiReportAsyncService {

    private static final String SYSTEM_PROMPT =
            """
            당신은 암호화폐 트레이딩 멘토입니다. 트레이더의 거래 정보와 매매 원칙을 분석하여 피드백을 제공합니다.
            반드시 아래 JSON 형식으로만 응답하세요:
            {
              "hashtags": ["형용사1", "형용사2", "형용사3", "형용사4", "형용사5"],
              "goodPoints": ["잘한 점 1", "잘한 점 2"],
              "badPoints": ["아쉬운 점 1", "아쉬운 점 2"],
              "recommendedRule": {
                "type": "BUY 또는 SELL",
                "content": "추천 매매 원칙 내용"
              }
            }
            hashtags는 준수율, 매매 원칙 준수 패턴, 복기 점수를 분석하여 트레이더의 매매 성향을 나타내는 짧은 형용사형 단어 정확히 5개를 작성하세요.
            예시: "꼼꼼한", "이성적인", "신중한", "엄격한", "우직한", "과감한", "감정적인", "충동적인"
            단어 형태로만 작성하고, 문장이나 설명은 포함하지 마세요. 반드시 5개여야 합니다.
            모든 응답은 한국어로 작성하세요.
            """;

    private final AiReportRepository aiReportRepository;
    private final TradeRepository tradeRepository;
    private final RuleRepository ruleRepository;
    private final TradeReviewRepository tradeReviewRepository;
    private final ReviewScoreRepository reviewScoreRepository;
    private final OpenAiClient openAiClient;

    @Async("aiReportTaskExecutor")
    @Transactional
    public void processReport(Long tradeId) {
        log.info("[AI] 리포트 생성 시작 tradeId={}", tradeId);
        AiReport report = aiReportRepository.findByTradeId(tradeId).orElse(null);
        if (report == null) {
            log.error("[AI] 리포트를 찾을 수 없습니다 tradeId={}", tradeId);
            return;
        }
        try {
            Trade trade = tradeRepository.findById(tradeId).orElseThrow();
            Optional<TradeReview> reviewOpt = tradeReviewRepository.findByTradeId(tradeId);
            List<Rule> rules = fetchRules(trade, reviewOpt);

            List<ReviewScore> scores = reviewOpt.isPresent()
                    ? reviewScoreRepository.findAllByReviewReviewId(reviewOpt.get().getReviewId())
                    : List.of();

            Double complianceRate = null;
            Grade grade = null;

            if (!scores.isEmpty()) {
                double total = scores.stream().mapToInt(ReviewScore::getScore).sum();
                complianceRate = (total / (scores.size() * 5.0)) * 100;
                grade = Grade.from(complianceRate);
            }

            log.info("[AI] OpenAI 호출 시작 tradeId={}, complianceRate={}", tradeId, complianceRate);
            String userPrompt = buildUserPrompt(trade, rules, reviewOpt, scores, complianceRate);
            String rawContent = openAiClient.requestCompletion(SYSTEM_PROMPT, userPrompt);
            log.info("[AI] OpenAI 응답 완료 tradeId={}", tradeId);

            report.complete(rawContent, complianceRate, grade);
            log.info("[AI] 리포트 저장 완료 tradeId={}, grade={}", tradeId, grade);
        } catch (Exception e) {
            log.error("[AI] 리포트 처리 실패 tradeId={}", tradeId, e);
            report.fail();
        }
    }

    private List<Rule> fetchRules(Trade trade, Optional<TradeReview> reviewOpt) {
        Long ruleSetId = trade.getRuleSetId();
        if (ruleSetId == null) {
            ruleSetId = reviewOpt.map(TradeReview::getRuleSetId).orElse(null);
        }
        if (ruleSetId == null) {
            return List.of();
        }
        return ruleRepository.findByRuleSetIdAndIsActiveTrueOrderByOrderIndexAsc(ruleSetId);
    }

    private String buildUserPrompt(Trade trade, List<Rule> rules,
            Optional<TradeReview> reviewOpt, List<ReviewScore> scores, Double complianceRate) {
        StringBuilder sb = new StringBuilder();

        sb.append("[거래 정보]\n");
        sb.append("- 거래 유형: ").append(trade.getTradeType()).append("\n");
        sb.append("- 종목: ").append(trade.getCoinType()).append("\n");
        sb.append("- 가격: ").append(trade.getPrice()).append("\n");
        sb.append("- 수량: ").append(trade.getQuantity()).append("\n");
        sb.append("- 거래 일시: ").append(trade.getTradedAt()).append("\n\n");

        if (!rules.isEmpty()) {
            sb.append("[적용된 매매 원칙]\n");
            rules.forEach(rule ->
                    sb.append("- [").append(rule.getType()).append("] ").append(rule.getContent()).append("\n")
            );
            sb.append("\n");
        }

        if (reviewOpt.isPresent()) {
            TradeReview review = reviewOpt.get();

            if (review.getContent() != null && !review.getContent().isBlank()) {
                sb.append("[복기 메모]\n");
                sb.append(review.getContent()).append("\n\n");
            }

            if (complianceRate != null && !rules.isEmpty()) {
                Map<Long, Rule> ruleMap = rules.stream()
                        .collect(Collectors.toMap(Rule::getId, r -> r));

                sb.append("[복기 점수]\n");
                sb.append(String.format("- 전체 준수율: %.1f%%\n", complianceRate));
                scores.forEach(score -> {
                    Rule rule = ruleMap.get(score.getRuleId());
                    String ruleName = rule != null ? rule.getContent() : "규칙 #" + score.getRuleId();
                    sb.append("- ").append(ruleName).append(": ").append(score.getScore()).append("/5\n");
                });
                sb.append("\n");
            }
        }

        return sb.toString();
    }
}
