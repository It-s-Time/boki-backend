package com.boki.backend.domain.review.service;

import com.boki.backend.domain.review.dto.request.ReviewSaveRequest;
import com.boki.backend.domain.review.dto.response.ReviewResponse;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface TradeReviewService {

    ReviewResponse createReview(Long memberId, Long tradeId, ReviewSaveRequest request, List<MultipartFile> images);

    ReviewResponse getReview(Long memberId, Long tradeId);

    ReviewResponse updateReview(Long memberId, Long reviewId, ReviewSaveRequest request, List<MultipartFile> images);

    void deleteReview(Long memberId, Long tradeId);
}
