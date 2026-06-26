package com.boki.backend.domain.review.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.boki.backend.domain.auth.jwt.JwtTokenProvider;
import com.boki.backend.domain.review.repository.ReviewImageRepository;
import com.boki.backend.domain.review.repository.ReviewScoreRepository;
import com.boki.backend.domain.review.repository.TradeReviewRepository;
import com.boki.backend.domain.review.service.ReviewImageStorage;
import com.boki.backend.domain.review.service.ReviewImageUploadResult;
import com.boki.backend.domain.ruleset.entity.Rule;
import com.boki.backend.domain.ruleset.entity.RuleSet;
import com.boki.backend.domain.ruleset.entity.RuleSetType;
import com.boki.backend.domain.ruleset.entity.RuleType;
import com.boki.backend.domain.ruleset.repository.RuleRepository;
import com.boki.backend.domain.ruleset.repository.RuleSetRepository;
import com.boki.backend.domain.trade.entity.Trade;
import com.boki.backend.domain.trade.entity.TradeInputType;
import com.boki.backend.domain.trade.entity.TradeType;
import com.boki.backend.domain.trade.repository.TradeRepository;
import com.boki.backend.global.storage.entity.S3CleanupStatus;
import com.boki.backend.global.storage.repository.S3CleanupTaskRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.web.multipart.MultipartFile;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class TradeReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private RuleSetRepository ruleSetRepository;

    @Autowired
    private RuleRepository ruleRepository;

    @Autowired
    private TradeReviewRepository tradeReviewRepository;

    @Autowired
    private ReviewScoreRepository reviewScoreRepository;

    @MockitoSpyBean
    private ReviewImageRepository reviewImageRepository;

    @MockitoSpyBean
    private S3CleanupTaskRepository cleanupTaskRepository;

    @MockitoBean
    private ReviewImageStorage reviewImageStorage;

    @BeforeEach
    void setUp() {
        cleanupTaskRepository.deleteAll();
        reviewImageRepository.deleteAll();
        reviewScoreRepository.deleteAll();
        tradeReviewRepository.deleteAll();
        tradeRepository.deleteAll();
        ruleRepository.deleteAll();
        ruleSetRepository.deleteAll();

        when(reviewImageStorage.upload(any(MultipartFile.class), anyLong(), anyLong(), anyInt()))
                .thenAnswer(invocation -> new ReviewImageUploadResult(
                        "object-" + invocation.getArgument(3),
                        "https://cdn.example.com/object-" + invocation.getArgument(3)
                ));
    }

    @Test
    void startReviewEndpointIsNotProvided() throws Exception {
        Trade trade = createTrade(1L, null, TradeType.BUY);

        mockMvc.perform(get("/api/trades/{tradeId}/reviews/start", trade.getTradeId())
                        .header("Authorization", bearer(1L)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createReviewUploadsImagesAndAssignsRuleSetToTradeWhenMissing() throws Exception {
        RuleSet ruleSet = createRuleSet(1L);
        Rule buyRule1 = createRule(ruleSet, RuleType.BUY, "기술적 분석 지표 3개 이상 확인하기", 0, true);
        Rule buyRule2 = createRule(ruleSet, RuleType.BUY, "매수할 때 분할매수로 진행하기", 1, true);
        Trade trade = createTrade(1L, null, TradeType.BUY);

        mockMvc.perform(multipart("/api/trades/{tradeId}/reviews", trade.getTradeId())
                        .file(jsonPart("""
                                {
                                  "ruleSetId": %d,
                                  "scores": [
                                    { "ruleId": %d, "score": 5 },
                                    { "ruleId": %d, "score": 3 }
                                  ],
                                  "content": "오늘 매매는 원칙은 지켰지만 익절 판단이 늦었다."
                                }
                                """.formatted(ruleSet.getId(), buyRule1.getId(), buyRule2.getId())))
                        .file(imagePart("images", "chart.png", "image/png"))
                        .header("Authorization", bearer(1L)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.tradeId", is(trade.getTradeId().intValue())))
                .andExpect(jsonPath("$.result.ruleSetId", is(ruleSet.getId().intValue())))
                .andExpect(jsonPath("$.result.content", is("오늘 매매는 원칙은 지켰지만 익절 판단이 늦었다.")))
                .andExpect(jsonPath("$.result.scores", hasSize(2)))
                .andExpect(jsonPath("$.result.scores[0].ruleContent", is("기술적 분석 지표 3개 이상 확인하기")))
                .andExpect(jsonPath("$.result.imageUrls[0]", is("https://cdn.example.com/object-0")));

        mockMvc.perform(get("/api/trades/{tradeId}/reviews", trade.getTradeId())
                        .header("Authorization", bearer(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.imageUrls[0]", is("https://cdn.example.com/object-0")))
                .andExpect(jsonPath("$.result.scores", hasSize(2)));

        Trade updatedTrade = tradeRepository.findById(trade.getTradeId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(updatedTrade.getRuleSetId()).isEqualTo(ruleSet.getId());
    }

    @Test
    void createReviewAcceptsSwaggerOctetStreamRequestPart() throws Exception {
        RuleSet ruleSet = createRuleSet(1L);
        Rule buyRule1 = createRule(ruleSet, RuleType.BUY, "기술적 분석 지표 3개 이상 확인하기", 0, true);
        Rule buyRule2 = createRule(ruleSet, RuleType.BUY, "매수할 때 분할매수로 진행하기", 1, true);
        Trade trade = createTrade(1L, null, TradeType.BUY);

        mockMvc.perform(multipart("/api/trades/{tradeId}/reviews", trade.getTradeId())
                        .file(jsonPart("""
                                {
                                  "ruleSetId": %d,
                                  "scores": [
                                    { "ruleId": %d, "score": 5 },
                                    { "ruleId": %d, "score": 3 }
                                  ],
                                  "content": "Swagger multipart 테스트"
                                }
                                """.formatted(ruleSet.getId(), buyRule1.getId(), buyRule2.getId()),
                                MediaType.APPLICATION_OCTET_STREAM_VALUE))
                        .header("Authorization", bearer(1L)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.content", is("Swagger multipart 테스트")))
                .andExpect(jsonPath("$.result.scores", hasSize(2)));
    }

    @Test
    void patchWithoutImagesKeepsExistingImagesAndPatchWithImagesReplacesThem() throws Exception {
        RuleSet ruleSet = createRuleSet(1L);
        Rule buyRule1 = createRule(ruleSet, RuleType.BUY, "기술적 분석 지표 3개 이상 확인하기", 0, true);
        Rule buyRule2 = createRule(ruleSet, RuleType.BUY, "매수할 때 분할매수로 진행하기", 1, true);
        Trade trade = createTrade(1L, ruleSet.getId(), TradeType.BUY);
        Long reviewId = createReview(trade, ruleSet, buyRule1, buyRule2);

        MockMultipartHttpServletRequestBuilder patchWithoutImages =
                multipart("/api/reviews/{reviewId}", reviewId);
        patchWithoutImages.with(request -> {
            request.setMethod("PATCH");
            return request;
        });

        mockMvc.perform(patchWithoutImages
                        .file(jsonPart("""
                                {
                                  "ruleSetId": %d,
                                  "scores": [
                                    { "ruleId": %d, "score": 4 },
                                    { "ruleId": %d, "score": 4 }
                                  ],
                                  "content": "수정된 복기 내용",
                                  "replaceImages": false
                                }
                                """.formatted(ruleSet.getId(), buyRule1.getId(), buyRule2.getId())))
                        .header("Authorization", bearer(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content", is("수정된 복기 내용")))
                .andExpect(jsonPath("$.result.imageUrls[0]", is("https://cdn.example.com/object-0")));

        verify(reviewImageStorage, never()).delete(anyString(), anyString());

        MockMultipartHttpServletRequestBuilder patchWithImages =
                multipart("/api/reviews/{reviewId}", reviewId);
        patchWithImages.with(request -> {
            request.setMethod("PATCH");
            return request;
        });

        mockMvc.perform(patchWithImages
                        .file(jsonPart("""
                                {
                                  "ruleSetId": %d,
                                  "scores": [
                                    { "ruleId": %d, "score": 2 },
                                    { "ruleId": %d, "score": 5 }
                                  ],
                                  "content": "이미지 교체",
                                  "replaceImages": false
                                }
                                """.formatted(ruleSet.getId(), buyRule1.getId(), buyRule2.getId())))
                        .file(imagePart("images", "new-chart.png", "image/png"))
                        .header("Authorization", bearer(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content", is("이미지 교체")))
                .andExpect(jsonPath("$.result.imageUrls", hasSize(1)));

        verify(reviewImageStorage, never()).delete(anyString(), anyString());
        org.assertj.core.api.Assertions.assertThat(cleanupTaskRepository.findAll())
                .singleElement()
                .satisfies(task -> {
                    org.assertj.core.api.Assertions.assertThat(task.getObjectKey()).isEqualTo("object-0");
                    org.assertj.core.api.Assertions.assertThat(task.getStatus()).isEqualTo(S3CleanupStatus.PENDING);
                });
    }

    @Test
    void patchWithReplaceImagesTrueAndNoImagesDeletesExistingImages() throws Exception {
        RuleSet ruleSet = createRuleSet(1L);
        Rule buyRule1 = createRule(ruleSet, RuleType.BUY, "기술적 분석 지표 3개 이상 확인하기", 0, true);
        Rule buyRule2 = createRule(ruleSet, RuleType.BUY, "매수할 때 분할매수로 진행하기", 1, true);
        Trade trade = createTrade(1L, ruleSet.getId(), TradeType.BUY);
        Long reviewId = createReview(trade, ruleSet, buyRule1, buyRule2);

        MockMultipartHttpServletRequestBuilder patch = multipart("/api/reviews/{reviewId}", reviewId);
        patch.with(request -> {
            request.setMethod("PATCH");
            return request;
        });

        mockMvc.perform(patch
                        .file(jsonPart("""
                                {
                                  "ruleSetId": %d,
                                  "scores": [
                                    { "ruleId": %d, "score": 5 },
                                    { "ruleId": %d, "score": 5 }
                                  ],
                                  "content": "이미지 전체 삭제",
                                  "replaceImages": true
                                }
                                """.formatted(ruleSet.getId(), buyRule1.getId(), buyRule2.getId())))
                        .header("Authorization", bearer(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.imageUrls", hasSize(0)));

        verify(reviewImageStorage, never()).delete(anyString(), anyString());
        org.assertj.core.api.Assertions.assertThat(cleanupTaskRepository.findAll())
                .singleElement()
                .satisfies(task -> {
                    org.assertj.core.api.Assertions.assertThat(task.getObjectKey()).isEqualTo("object-0");
                    org.assertj.core.api.Assertions.assertThat(task.getStatus()).isEqualTo(S3CleanupStatus.PENDING);
                });
    }

    @Test
    void deleteReviewDeletesDatabaseRowsAndCreatesS3CleanupTask() throws Exception {
        RuleSet ruleSet = createRuleSet(1L);
        Rule buyRule1 = createRule(ruleSet, RuleType.BUY, "기술적 분석 지표 3개 이상 확인하기", 0, true);
        Rule buyRule2 = createRule(ruleSet, RuleType.BUY, "매수할 때 분할매수로 진행하기", 1, true);
        Trade trade = createTrade(1L, ruleSet.getId(), TradeType.BUY);
        createReview(trade, ruleSet, buyRule1, buyRule2);

        mockMvc.perform(delete("/api/trades/{tradeId}/reviews", trade.getTradeId())
                        .header("Authorization", bearer(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess", is(true)));

        org.assertj.core.api.Assertions.assertThat(tradeReviewRepository.findByTradeId(trade.getTradeId())).isEmpty();
        org.assertj.core.api.Assertions.assertThat(reviewScoreRepository.findAll()).isEmpty();
        org.assertj.core.api.Assertions.assertThat(reviewImageRepository.findAll()).isEmpty();
        verify(reviewImageStorage, never()).delete(anyString(), anyString());
        org.assertj.core.api.Assertions.assertThat(cleanupTaskRepository.findAll())
                .singleElement()
                .satisfies(task -> {
                    org.assertj.core.api.Assertions.assertThat(task.getObjectKey()).isEqualTo("object-0");
                    org.assertj.core.api.Assertions.assertThat(task.getStatus()).isEqualTo(S3CleanupStatus.PENDING);
                });
    }

    @Test
    void deleteReviewIsIdempotent() throws Exception {
        RuleSet ruleSet = createRuleSet(1L);
        Rule buyRule1 = createRule(ruleSet, RuleType.BUY, "기술적 분석 지표 3개 이상 확인하기", 0, true);
        Rule buyRule2 = createRule(ruleSet, RuleType.BUY, "매수할 때 분할매수로 진행하기", 1, true);
        Trade trade = createTrade(1L, ruleSet.getId(), TradeType.BUY);
        createReview(trade, ruleSet, buyRule1, buyRule2);

        mockMvc.perform(delete("/api/trades/{tradeId}/reviews", trade.getTradeId())
                        .header("Authorization", bearer(1L)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/trades/{tradeId}/reviews", trade.getTradeId())
                        .header("Authorization", bearer(1L)))
                .andExpect(status().isOk());

        org.assertj.core.api.Assertions.assertThat(cleanupTaskRepository.findAll()).hasSize(1);
    }

    @Test
    void cleanupTaskSaveFailureRollsBackReviewDeletion() throws Exception {
        RuleSet ruleSet = createRuleSet(1L);
        Rule buyRule1 = createRule(ruleSet, RuleType.BUY, "기술적 분석 지표 3개 이상 확인하기", 0, true);
        Rule buyRule2 = createRule(ruleSet, RuleType.BUY, "매수할 때 분할매수로 진행하기", 1, true);
        Trade trade = createTrade(1L, ruleSet.getId(), TradeType.BUY);
        createReview(trade, ruleSet, buyRule1, buyRule2);
        doThrow(new RuntimeException("cleanup task DB failure"))
                .when(cleanupTaskRepository)
                .saveAll(any());

        mockMvc.perform(delete("/api/trades/{tradeId}/reviews", trade.getTradeId())
                        .header("Authorization", bearer(1L)))
                .andExpect(status().isInternalServerError());

        org.assertj.core.api.Assertions.assertThat(tradeReviewRepository.findByTradeId(trade.getTradeId())).isPresent();
        org.assertj.core.api.Assertions.assertThat(reviewScoreRepository.findAll()).hasSize(2);
        org.assertj.core.api.Assertions.assertThat(reviewImageRepository.findAll()).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(cleanupTaskRepository.findAll()).isEmpty();
    }

    @Test
    void invalidRuleScoreFailsWhenRuleDoesNotBelongToReviewRuleSet() throws Exception {
        RuleSet ruleSet = createRuleSet(1L);
        Rule buyRule = createRule(ruleSet, RuleType.BUY, "기술적 분석 지표 3개 이상 확인하기", 0, true);
        RuleSet otherRuleSet = createRuleSet(1L);
        Rule otherRule = createRule(otherRuleSet, RuleType.BUY, "다른 룰셋 원칙", 0, true);
        Trade trade = createTrade(1L, ruleSet.getId(), TradeType.BUY);

        mockMvc.perform(multipart("/api/trades/{tradeId}/reviews", trade.getTradeId())
                        .file(jsonPart("""
                                {
                                  "ruleSetId": %d,
                                  "scores": [
                                    { "ruleId": %d, "score": 5 },
                                    { "ruleId": %d, "score": 3 }
                                  ],
                                  "content": "잘못된 점수"
                                }
                                """.formatted(ruleSet.getId(), buyRule.getId(), otherRule.getId())))
                        .header("Authorization", bearer(1L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("REVIEW400_3")));
    }

    @Test
    void duplicateReviewCreationFails() throws Exception {
        RuleSet ruleSet = createRuleSet(1L);
        Rule buyRule1 = createRule(ruleSet, RuleType.BUY, "기술적 분석 지표 3개 이상 확인하기", 0, true);
        Rule buyRule2 = createRule(ruleSet, RuleType.BUY, "매수할 때 분할매수로 진행하기", 1, true);
        Trade trade = createTrade(1L, ruleSet.getId(), TradeType.BUY);
        createReview(trade, ruleSet, buyRule1, buyRule2);

        mockMvc.perform(multipart("/api/trades/{tradeId}/reviews", trade.getTradeId())
                        .file(reviewRequest(ruleSet, buyRule1, buyRule2, "중복 복기"))
                        .header("Authorization", bearer(1L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("REVIEW409")));
    }

    @Test
    void imageValidationFailsForTooManyImages() throws Exception {
        RuleSet ruleSet = createRuleSet(1L);
        Rule buyRule1 = createRule(ruleSet, RuleType.BUY, "기술적 분석 지표 3개 이상 확인하기", 0, true);
        Rule buyRule2 = createRule(ruleSet, RuleType.BUY, "매수할 때 분할매수로 진행하기", 1, true);
        Trade trade = createTrade(1L, ruleSet.getId(), TradeType.BUY);

        mockMvc.perform(multipart("/api/trades/{tradeId}/reviews", trade.getTradeId())
                        .file(reviewRequest(ruleSet, buyRule1, buyRule2, "이미지 개수 초과"))
                        .file(imagePart("images", "chart1.png", "image/png"))
                        .file(imagePart("images", "chart2.png", "image/png"))
                        .file(imagePart("images", "chart3.png", "image/png"))
                        .file(imagePart("images", "chart4.png", "image/png"))
                        .header("Authorization", bearer(1L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("REVIEW400_4")));
    }

    @Test
    void failedUploadCompensationCreatesCleanupTaskInIndependentTransaction() throws Exception {
        RuleSet ruleSet = createRuleSet(1L);
        Rule buyRule1 = createRule(ruleSet, RuleType.BUY, "기술적 분석 지표 3개 이상 확인하기", 0, true);
        Rule buyRule2 = createRule(ruleSet, RuleType.BUY, "매수할 때 분할매수로 진행하기", 1, true);
        Trade trade = createTrade(1L, ruleSet.getId(), TradeType.BUY);
        doThrow(new RuntimeException("review image DB failure"))
                .when(reviewImageRepository)
                .saveAll(any());
        doThrow(new RuntimeException("S3 compensation failure"))
                .when(reviewImageStorage)
                .delete("boki-test-bucket", "object-0");

        mockMvc.perform(multipart("/api/trades/{tradeId}/reviews", trade.getTradeId())
                        .file(reviewRequest(ruleSet, buyRule1, buyRule2, "보상 삭제 실패"))
                        .file(imagePart("images", "chart.png", "image/png"))
                        .header("Authorization", bearer(1L)))
                .andExpect(status().isInternalServerError());

        org.assertj.core.api.Assertions.assertThat(tradeReviewRepository.findByTradeId(trade.getTradeId())).isEmpty();
        org.assertj.core.api.Assertions.assertThat(cleanupTaskRepository.findAll())
                .singleElement()
                .satisfies(task -> {
                    org.assertj.core.api.Assertions.assertThat(task.getObjectKey()).isEqualTo("object-0");
                    org.assertj.core.api.Assertions.assertThat(task.getStatus()).isEqualTo(S3CleanupStatus.PENDING);
                });
    }

    private Long createReview(Trade trade, RuleSet ruleSet, Rule buyRule1, Rule buyRule2) throws Exception {
        String response = mockMvc.perform(multipart("/api/trades/{tradeId}/reviews", trade.getTradeId())
                        .file(reviewRequest(ruleSet, buyRule1, buyRule2, "초기 복기"))
                        .file(imagePart("images", "chart.png", "image/png"))
                        .header("Authorization", bearer(1L)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number reviewId = com.jayway.jsonpath.JsonPath.read(response, "$.result.reviewId");
        return reviewId.longValue();
    }

    private MockMultipartFile reviewRequest(RuleSet ruleSet, Rule buyRule1, Rule buyRule2, String content) {
        return jsonPart("""
                {
                  "ruleSetId": %d,
                  "scores": [
                    { "ruleId": %d, "score": 5 },
                    { "ruleId": %d, "score": 3 }
                  ],
                  "content": "%s"
                }
                """.formatted(ruleSet.getId(), buyRule1.getId(), buyRule2.getId(), content));
    }

    private MockMultipartFile jsonPart(String json) {
        return jsonPart(json, MediaType.APPLICATION_JSON_VALUE);
    }

    private MockMultipartFile jsonPart(String json, String contentType) {
        return new MockMultipartFile(
                "request",
                "request.json",
                contentType,
                json.getBytes(StandardCharsets.UTF_8)
        );
    }

    private MockMultipartFile imagePart(String name, String originalFilename, String contentType) {
        return new MockMultipartFile(
                name,
                originalFilename,
                contentType,
                "image-content".getBytes(StandardCharsets.UTF_8)
        );
    }

    private RuleSet createRuleSet(Long memberId) {
        return ruleSetRepository.save(RuleSet.builder()
                .memberId(memberId)
                .name("나의 안정형 전략")
                .type(RuleSetType.CUSTOM)
                .build());
    }

    private Rule createRule(RuleSet ruleSet, RuleType type, String content, int orderIndex, boolean active) {
        return ruleRepository.save(Rule.builder()
                .ruleSet(ruleSet)
                .type(type)
                .content(content)
                .orderIndex(orderIndex)
                .isActive(active)
                .build());
    }

    private Trade createTrade(Long memberId, Long ruleSetId, TradeType tradeType) {
        return tradeRepository.save(Trade.builder()
                .memberId(memberId)
                .ruleSetId(ruleSetId)
                .tradeType(tradeType)
                .inputType(TradeInputType.MANUAL)
                .coinType("BTC")
                .price(BigDecimal.valueOf(103_403_000))
                .quantity(BigDecimal.valueOf(0.01))
                .tradedAt(LocalDateTime.of(2026, 6, 6, 14, 32))
                .build());
    }

    private String bearer(Long memberId) {
        return "Bearer " + jwtTokenProvider.createAccessToken(memberId);
    }
}
