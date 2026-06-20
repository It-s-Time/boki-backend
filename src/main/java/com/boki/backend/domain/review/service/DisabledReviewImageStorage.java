package com.boki.backend.domain.review.service;

import com.boki.backend.domain.review.exception.ReviewErrorCode;
import com.boki.backend.global.apiPayload.exception.GeneralException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@ConditionalOnExpression("!'${boki.aws.s3.enabled:false}'.equalsIgnoreCase('true')")
public class DisabledReviewImageStorage implements ReviewImageStorage {

    @PostConstruct
    void logStorageMode() {
        log.warn("Review image storage is DISABLED. Set AWS_S3_ENABLED=true to upload review images to S3.");
    }

    @Override
    public ReviewImageUploadResult upload(MultipartFile file, Long memberId, Long reviewId, int orderIndex) {
        log.warn("Review image upload requested while S3 storage is disabled. memberId={}, reviewId={}", memberId, reviewId);
        throw new GeneralException(ReviewErrorCode.REVIEW_IMAGE_UPLOAD_FAILED);
    }

    @Override
    public void delete(String bucket, String objectKey) {
        throw new GeneralException(ReviewErrorCode.REVIEW_IMAGE_DELETE_FAILED);
    }
}
