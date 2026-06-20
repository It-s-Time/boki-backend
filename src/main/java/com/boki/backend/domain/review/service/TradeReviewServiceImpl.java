package com.boki.backend.domain.review.service;

import com.boki.backend.domain.review.dto.request.ReviewSaveRequest;
import com.boki.backend.domain.review.dto.request.ReviewScoreRequest;
import com.boki.backend.domain.review.dto.response.ReviewResponse;
import com.boki.backend.domain.review.dto.response.ReviewScoreResponse;
import com.boki.backend.domain.review.entity.ReviewImage;
import com.boki.backend.domain.review.entity.ReviewScore;
import com.boki.backend.domain.review.entity.TradeReview;
import com.boki.backend.domain.review.exception.ReviewErrorCode;
import com.boki.backend.domain.review.repository.ReviewImageRepository;
import com.boki.backend.domain.review.repository.ReviewScoreRepository;
import com.boki.backend.domain.review.repository.TradeReviewRepository;
import com.boki.backend.domain.ruleset.entity.Rule;
import com.boki.backend.domain.ruleset.entity.RuleSet;
import com.boki.backend.domain.ruleset.entity.RuleSetType;
import com.boki.backend.domain.ruleset.entity.RuleType;
import com.boki.backend.domain.ruleset.repository.RuleRepository;
import com.boki.backend.domain.ruleset.repository.RuleSetRepository;
import com.boki.backend.domain.trade.entity.Trade;
import com.boki.backend.domain.trade.exception.TradeErrorCode;
import com.boki.backend.domain.trade.repository.TradeRepository;
import com.boki.backend.global.apiPayload.exception.GeneralException;
import com.boki.backend.global.config.S3Properties;
import com.boki.backend.global.storage.service.S3CleanupTaskService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TradeReviewServiceImpl implements TradeReviewService {

    private static final int MAX_IMAGE_COUNT = 3;
    private static final long MAX_IMAGE_SIZE = 5L * 1024L * 1024L; //5MB로 고정
    private static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final TradeRepository tradeRepository;
    private final RuleSetRepository ruleSetRepository;
    private final RuleRepository ruleRepository;
    private final TradeReviewRepository tradeReviewRepository;
    private final ReviewScoreRepository reviewScoreRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final ReviewImageStorage reviewImageStorage;
    private final S3CleanupTaskService s3CleanupTaskService;
    private final S3Properties s3Properties;

    @Override
    @Transactional
    public ReviewResponse createReview(
            Long memberId,
            Long tradeId,
            ReviewSaveRequest request,
            List<MultipartFile> images
    ) {
        Trade trade = getOwnedTrade(memberId, tradeId);
        if (tradeReviewRepository.existsByTradeId(tradeId)) {
            throw new GeneralException(ReviewErrorCode.REVIEW_ALREADY_EXISTS);
        }

        RuleSet ruleSet = getAccessibleRuleSet(memberId, request.ruleSetId());
        validateAndApplyTradeRuleSet(trade, ruleSet.getId());
        List<Rule> activeRules = validateScores(ruleSet.getId(), trade, request.scores());
        List<MultipartFile> imageFiles = validateImages(images);

        TradeReview review = tradeReviewRepository.save(TradeReview.builder()
                .tradeId(trade.getTradeId())
                .memberId(memberId)
                .ruleSetId(ruleSet.getId())
                .content(request.content())
                .build());

        saveScores(review, request.scores());
        uploadAndSaveImages(review, imageFiles);

        return toResponse(review, activeRules);
    }

    @Override
    public ReviewResponse getReview(Long memberId, Long tradeId) {
        TradeReview review = tradeReviewRepository.findByTradeIdAndMemberId(tradeId, memberId)
                .orElseThrow(() -> new GeneralException(ReviewErrorCode.REVIEW_NOT_FOUND));
        return toResponse(review);
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(
            Long memberId,
            Long reviewId,
            ReviewSaveRequest request,
            List<MultipartFile> images
    ) {
        TradeReview review = getOwnedReview(memberId, reviewId);
        if (!review.getRuleSetId().equals(request.ruleSetId())) {
            throw new GeneralException(ReviewErrorCode.REVIEW_RULE_SET_MISMATCH);
        }

        Trade trade = getOwnedTrade(memberId, review.getTradeId());
        List<Rule> activeRules = validateScores(review.getRuleSetId(), trade, request.scores());
        List<MultipartFile> imageFiles = validateImages(images);

        review.updateContent(request.content());
        replaceScores(review, request.scores());

        if (!imageFiles.isEmpty()) {
            replaceImages(review, imageFiles);
        } else if (request.shouldReplaceImages()) {
            deleteImages(review);
        }

        return toResponse(review, activeRules);
    }

    @Override
    @Transactional
    public void deleteReview(Long memberId, Long tradeId) {
        getOwnedTrade(memberId, tradeId);
        TradeReview review = tradeReviewRepository.findByTradeIdAndMemberId(tradeId, memberId)
                .orElse(null);
        if (review == null) {
            return;
        }
        deleteImages(review);
        reviewScoreRepository.deleteAllByReviewReviewId(review.getReviewId());
        tradeReviewRepository.delete(review);
    }




    //------private method

    private Trade getOwnedTrade(Long memberId, Long tradeId) {
        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new GeneralException(TradeErrorCode.TRADE_NOT_FOUND));
        if (!trade.getMemberId().equals(memberId)) {
            throw new GeneralException(TradeErrorCode.TRADE_FORBIDDEN);
        }
        return trade;
    }

    private TradeReview getOwnedReview(Long memberId, Long reviewId) {
        TradeReview review = tradeReviewRepository.findById(reviewId)
                .orElseThrow(() -> new GeneralException(ReviewErrorCode.REVIEW_NOT_FOUND));
        if (!review.getMemberId().equals(memberId)) {
            throw new GeneralException(TradeErrorCode.TRADE_FORBIDDEN);
        }
        return review;
    }

    private RuleSet getAccessibleRuleSet(Long memberId, Long ruleSetId) {
        RuleSet ruleSet = ruleSetRepository.findById(ruleSetId)
                .orElseThrow(() -> new GeneralException(ReviewErrorCode.REVIEW_RULE_SET_FORBIDDEN));
        if (ruleSet.getType() == RuleSetType.CUSTOM && !ruleSet.getMemberId().equals(memberId)) {
            throw new GeneralException(ReviewErrorCode.REVIEW_RULE_SET_FORBIDDEN);
        }
        return ruleSet;
    }

    private void validateAndApplyTradeRuleSet(Trade trade, Long ruleSetId) {
        if (trade.getRuleSetId() == null) {
            trade.assignRuleSet(ruleSetId);
            return;
        }
        if (!trade.getRuleSetId().equals(ruleSetId)) {
            throw new GeneralException(ReviewErrorCode.REVIEW_RULE_SET_MISMATCH);
        }
    }

    private List<Rule> validateScores(Long ruleSetId, Trade trade, List<ReviewScoreRequest> scores) {
        List<Rule> activeRules = getActiveRules(ruleSetId, trade);
        Set<Long> expectedRuleIds = activeRules.stream()
                .map(Rule::getId)
                .collect(Collectors.toSet());
        List<Long> submittedRuleIds = scores.stream()
                .map(ReviewScoreRequest::ruleId)
                .toList();

        if (submittedRuleIds.size() != new HashSet<>(submittedRuleIds).size()) {
            throw new GeneralException(ReviewErrorCode.REVIEW_INVALID_RULE_SCORE);
        }
        if (!expectedRuleIds.equals(new HashSet<>(submittedRuleIds))) {
            throw new GeneralException(ReviewErrorCode.REVIEW_INVALID_RULE_SCORE);
        }

        return activeRules;
    }

    private List<Rule> getActiveRules(Long ruleSetId, Trade trade) {
        RuleType ruleType = RuleType.valueOf(trade.getTradeType().name());
        return ruleRepository.findByRuleSetIdAndTypeAndIsActiveTrueOrderByOrderIndexAsc(ruleSetId, ruleType);
    }

    private void saveScores(TradeReview review, List<ReviewScoreRequest> scores) {
        reviewScoreRepository.saveAll(scores.stream()
                .map(score -> ReviewScore.builder()
                        .review(review)
                        .ruleId(score.ruleId())
                        .score(score.score())
                        .build())
                .toList());
    }

    private void replaceScores(TradeReview review, List<ReviewScoreRequest> scores) {
        reviewScoreRepository.deleteAll(reviewScoreRepository.findAllByReviewReviewId(review.getReviewId()));
        reviewScoreRepository.flush();
        saveScores(review, scores);
    }

    private List<MultipartFile> validateImages(List<MultipartFile> images) {
        if (images == null) {
            return List.of();
        }

        List<MultipartFile> imageFiles = images.stream()
                .filter(file -> file != null && !file.isEmpty())
                .toList();

        if (imageFiles.size() > MAX_IMAGE_COUNT) {
            throw new GeneralException(ReviewErrorCode.REVIEW_IMAGE_LIMIT_EXCEEDED);
        }

        for (MultipartFile imageFile : imageFiles) {
            if (imageFile.getSize() > MAX_IMAGE_SIZE) {
                throw new GeneralException(ReviewErrorCode.REVIEW_IMAGE_SIZE_EXCEEDED);
            }
            if (!ALLOWED_IMAGE_CONTENT_TYPES.contains(imageFile.getContentType())) {
                throw new GeneralException(ReviewErrorCode.REVIEW_IMAGE_UNSUPPORTED_TYPE);
            }
        }

        return imageFiles;
    }

    private void replaceImages(TradeReview review, List<MultipartFile> imageFiles) {
        List<ReviewImage> previousImages = reviewImageRepository
                .findAllByReviewReviewIdOrderByOrderIndexAsc(review.getReviewId());
        List<String> previousObjectKeys = previousImages.stream()
                .map(ReviewImage::getObjectKey)
                .toList();

        uploadAndSaveImages(review, imageFiles);
        if (!previousObjectKeys.isEmpty()) {
            s3CleanupTaskService.enqueueAll(previousObjectKeys);
        }
        reviewImageRepository.deleteAll(previousImages);
    }

    private List<String> uploadAndSaveImages(TradeReview review, List<MultipartFile> imageFiles) {
        List<String> uploadedObjectKeys = new ArrayList<>();
        List<ReviewImage> reviewImages = new ArrayList<>();

        try {
            for (int i = 0; i < imageFiles.size(); i++) {
                MultipartFile imageFile = imageFiles.get(i);
                ReviewImageUploadResult uploadResult = reviewImageStorage.upload(
                        imageFile,
                        review.getMemberId(),
                        review.getReviewId(),
                        i
                );
                uploadedObjectKeys.add(uploadResult.objectKey());
                reviewImages.add(ReviewImage.builder()
                        .review(review)
                        .objectKey(uploadResult.objectKey())
                        .imageUrl(uploadResult.imageUrl())
                        .originalFilename(imageFile.getOriginalFilename())
                        .contentType(imageFile.getContentType())
                        .fileSize(imageFile.getSize())
                        .orderIndex(i)
                        .build());
            }
            reviewImageRepository.saveAll(reviewImages);
            reviewImageRepository.flush();
            return uploadedObjectKeys;
        } catch (RuntimeException exception) {
            safeDelete(uploadedObjectKeys);
            throw exception;
        }
    }

    private void deleteImages(TradeReview review) {
        List<ReviewImage> images = reviewImageRepository
                .findAllByReviewReviewIdOrderByOrderIndexAsc(review.getReviewId());
        List<String> objectKeys = images.stream()
                .map(ReviewImage::getObjectKey)
                .toList();
        if (!objectKeys.isEmpty()) {
            s3CleanupTaskService.enqueueAll(objectKeys);
        }
        reviewImageRepository.deleteAll(images);
    }

    private void safeDelete(List<String> objectKeys) {
        if (objectKeys.isEmpty()) {
            return;
        }
        List<String> failedObjectKeys = new ArrayList<>();
        for (String objectKey : objectKeys) {
            try {
                reviewImageStorage.delete(s3Properties.bucket(), objectKey);
            } catch (RuntimeException ignored) {
                failedObjectKeys.add(objectKey);
            }
        }
        if (!failedObjectKeys.isEmpty()) {
            try {
                s3CleanupTaskService.enqueueAllInNewTransaction(failedObjectKeys);
            } catch (RuntimeException exception) {
                log.error("Failed to persist S3 cleanup tasks. objectKeys={}", failedObjectKeys, exception);
            }
        }
    }

    private ReviewResponse toResponse(TradeReview review) {
        return toResponse(review, List.of());
    }

    private ReviewResponse toResponse(TradeReview review, List<Rule> alreadyLoadedActiveRules) {
        List<ReviewScore> scores = reviewScoreRepository.findAllByReviewReviewId(review.getReviewId());
        Map<Long, Rule> rulesById = loadRulesForScores(scores, alreadyLoadedActiveRules);
        List<String> imageUrls = reviewImageRepository
                .findAllByReviewReviewIdOrderByOrderIndexAsc(review.getReviewId())
                .stream()
                .map(ReviewImage::getImageUrl)
                .toList();

        return new ReviewResponse(
                review.getReviewId(),
                review.getTradeId(),
                review.getMemberId(),
                review.getRuleSetId(),
                review.getContent(),
                scores.stream()
                        .sorted(Comparator.comparingInt(score -> orderIndexOf(rulesById.get(score.getRuleId()))))
                        .map(score -> new ReviewScoreResponse(
                                score.getRuleId(),
                                contentOf(rulesById.get(score.getRuleId())),
                                score.getScore()
                        ))
                        .toList(),
                imageUrls,
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }

    private Map<Long, Rule> loadRulesForScores(List<ReviewScore> scores, List<Rule> alreadyLoadedActiveRules) {
        Set<Long> scoreRuleIds = scores.stream()
                .map(ReviewScore::getRuleId)
                .collect(Collectors.toSet());
        Map<Long, Rule> loaded = alreadyLoadedActiveRules.stream()
                .filter(rule -> scoreRuleIds.contains(rule.getId()))
                .collect(Collectors.toMap(Rule::getId, Function.identity()));

        List<Long> missingRuleIds = scoreRuleIds.stream()
                .filter(ruleId -> !loaded.containsKey(ruleId))
                .toList();
        if (!missingRuleIds.isEmpty()) {
            loaded.putAll(ruleRepository.findAllById(missingRuleIds).stream()
                    .collect(Collectors.toMap(Rule::getId, Function.identity())));
        }
        return loaded;
    }

    private int orderIndexOf(Rule rule) {
        return rule == null ? Integer.MAX_VALUE : rule.getOrderIndex();
    }

    private String contentOf(Rule rule) {
        return rule == null ? null : rule.getContent();
    }
}
