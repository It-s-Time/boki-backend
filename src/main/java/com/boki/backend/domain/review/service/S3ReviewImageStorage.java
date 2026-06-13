package com.boki.backend.domain.review.service;

import com.boki.backend.domain.review.exception.ReviewErrorCode;
import com.boki.backend.global.apiPayload.exception.GeneralException;
import com.boki.backend.global.config.S3Properties;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "boki.aws.s3", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class S3ReviewImageStorage implements ReviewImageStorage {

    private final S3Client s3Client;
    private final S3Properties properties;

    @PostConstruct
    void logStorageMode() {
        log.info("Review image storage is S3. bucket={}, region={}", properties.bucket(), properties.region());
    }

    @Override
    public ReviewImageUploadResult upload(MultipartFile file, Long memberId, Long reviewId, int orderIndex) {
        String objectKey = createObjectKey(file, memberId, reviewId);

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(properties.bucket())
                            .key(objectKey)
                            .contentType(file.getContentType())
                            .contentLength(file.getSize())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
        } catch (IOException | SdkException exception) {
            log.warn(
                    "Failed to upload review image to S3. bucket={}, key={}, contentType={}, fileSize={}",
                    properties.bucket(),
                    objectKey,
                    file.getContentType(),
                    file.getSize(),
                    exception
            );
            throw new GeneralException(ReviewErrorCode.REVIEW_IMAGE_UPLOAD_FAILED);
        }

        return new ReviewImageUploadResult(objectKey, buildImageUrl(objectKey));
    }

    @Override
    public void deleteAll(List<String> objectKeys) {
        for (String objectKey : objectKeys) {
            try {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(properties.bucket())
                        .key(objectKey)
                        .build());
            } catch (SdkException exception) {
                log.warn("Failed to delete review image from S3. bucket={}, key={}", properties.bucket(), objectKey, exception);
                throw new GeneralException(ReviewErrorCode.REVIEW_IMAGE_UPLOAD_FAILED);
            }
        }
    }

    private String createObjectKey(MultipartFile file, Long memberId, Long reviewId) {
        return "reviews/%d/%d/%s%s".formatted(
                memberId,
                reviewId,
                UUID.randomUUID(),
                extensionFor(file)
        );
    }

    private String extensionFor(MultipartFile file) {
        String contentType = file.getContentType();
        if ("image/jpeg".equals(contentType)) {
            return ".jpg";
        }
        if ("image/png".equals(contentType)) {
            return ".png";
        }
        if ("image/webp".equals(contentType)) {
            return ".webp";
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
    }

    private String buildImageUrl(String objectKey) {
        if (properties.publicBaseUrl() != null && !properties.publicBaseUrl().isBlank()) {
            return properties.publicBaseUrl().replaceAll("/+$", "") + "/" + objectKey;
        }
        return "https://%s.s3.%s.amazonaws.com/%s".formatted(
                properties.bucket(),
                properties.region(),
                objectKey
        );
    }
}
