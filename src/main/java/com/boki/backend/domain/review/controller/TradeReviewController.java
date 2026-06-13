package com.boki.backend.domain.review.controller;

import com.boki.backend.domain.review.dto.request.ReviewSaveRequest;
import com.boki.backend.domain.review.dto.request.ReviewMultipartRequest;
import com.boki.backend.domain.review.dto.response.ReviewResponse;
import com.boki.backend.domain.review.service.TradeReviewService;
import com.boki.backend.global.apiPayload.ApiResponse;
import com.boki.backend.global.apiPayload.code.GeneralErrorCode;
import com.boki.backend.global.apiPayload.code.GeneralSuccessCode;
import com.boki.backend.global.apiPayload.exception.GeneralException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Trade Review", description = "거래 복기 API")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class TradeReviewController {

    private final TradeReviewService tradeReviewService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @Operation(
            summary = "복기 생성",
            description = "원칙별 점수, 복기 내용, 이미지를 저장합니다.",
            requestBody = @RequestBody(
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = ReviewMultipartRequest.class),
                            encoding = @Encoding(name = "request", contentType = MediaType.APPLICATION_JSON_VALUE)
                    )
            )
    )
    @PostMapping(
            value = "/trades/{tradeId}/reviews",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @AuthenticationPrincipal Long memberId,
            @PathVariable @Positive(message = "거래 ID는 0보다 커야 합니다.") Long tradeId,
            @Parameter(hidden = true)
            @RequestPart("request") String requestJson,
            @Parameter(hidden = true)
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        ReviewSaveRequest request = parseRequest(requestJson);
        return ResponseEntity
                .status(GeneralSuccessCode.CREATED.getStatus())
                .body(ApiResponse.onSuccess(
                        GeneralSuccessCode.CREATED,
                        tradeReviewService.createReview(memberId, tradeId, request, images)
                ));
    }

    @Operation(summary = "복기 조회", description = "거래에 작성된 복기를 조회합니다.")
    @GetMapping("/trades/{tradeId}/reviews")
    public ResponseEntity<ApiResponse<ReviewResponse>> getReview(
            @AuthenticationPrincipal Long memberId,
            @PathVariable @Positive(message = "거래 ID는 0보다 커야 합니다.") Long tradeId
    ) {
        return ResponseEntity
                .status(GeneralSuccessCode.OK.getStatus())
                .body(ApiResponse.onSuccess(
                        GeneralSuccessCode.OK,
                        tradeReviewService.getReview(memberId, tradeId)
                ));
    }

    @Operation(
            summary = "복기 수정",
            description = "복기 내용과 원칙별 점수, 이미지를 수정합니다.",
            requestBody = @RequestBody(
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = ReviewMultipartRequest.class),
                            encoding = @Encoding(name = "request", contentType = MediaType.APPLICATION_JSON_VALUE)
                    )
            )
    )
    @PatchMapping(
            value = "/reviews/{reviewId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @AuthenticationPrincipal Long memberId,
            @PathVariable @Positive(message = "복기 ID는 0보다 커야 합니다.") Long reviewId,
            @Parameter(hidden = true)
            @RequestPart("request") String requestJson,
            @Parameter(hidden = true)
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        ReviewSaveRequest request = parseRequest(requestJson);
        return ResponseEntity
                .status(GeneralSuccessCode.OK.getStatus())
                .body(ApiResponse.onSuccess(
                        GeneralSuccessCode.OK,
                        tradeReviewService.updateReview(memberId, reviewId, request, images)
                ));
    }

    @Operation(summary = "복기 삭제", description = "거래에 작성된 복기와 연결 이미지를 삭제합니다.")
    @DeleteMapping("/trades/{tradeId}/reviews")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @AuthenticationPrincipal Long memberId,
            @PathVariable @Positive(message = "거래 ID는 0보다 커야 합니다.") Long tradeId
    ) {
        tradeReviewService.deleteReview(memberId, tradeId);
        return ResponseEntity
                .status(GeneralSuccessCode.DELETED.getStatus())
                .body(ApiResponse.onSuccess(GeneralSuccessCode.DELETED, null));
    }


    // ----------- private method

    private ReviewSaveRequest parseRequest(String requestJson) {
        try {
            ReviewSaveRequest request = objectMapper.readValue(requestJson, ReviewSaveRequest.class);
            validateRequest(request);
            return request;
        } catch (JsonProcessingException exception) {
            throw new GeneralException(GeneralErrorCode.BAD_REQUEST);
        }
    }

    private void validateRequest(ReviewSaveRequest request) {
        Set<ConstraintViolation<ReviewSaveRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }
}
