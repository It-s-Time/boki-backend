package com.boki.backend.global.storage.entity;

import com.boki.backend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "s3_cleanup_tasks",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_s3_cleanup_tasks_bucket_object_key", columnNames = {"bucket", "object_key"})
        },
        indexes = {
                @Index(name = "idx_s3_cleanup_tasks_status_next_retry", columnList = "status,next_retry_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class S3CleanupTask extends BaseEntity {

    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_ERROR_LENGTH = 1000;

    @Id
    @Column(name = "cleanup_task_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cleanupTaskId;

    @Column(name = "bucket", nullable = false, length = 255)
    private String bucket;

    @Column(name = "object_key", nullable = false, length = 500)
    private String objectKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private S3CleanupStatus status;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Builder
    private S3CleanupTask(
            String bucket,
            String objectKey,
            S3CleanupStatus status,
            Integer retryCount,
            LocalDateTime nextRetryAt,
            String lastError
    ) {
        this.bucket = bucket;
        this.objectKey = objectKey;
        this.status = status;
        this.retryCount = retryCount;
        this.nextRetryAt = nextRetryAt;
        this.lastError = lastError;
    }

    public static S3CleanupTask pending(String bucket, String objectKey, LocalDateTime now) {
        return S3CleanupTask.builder()
                .bucket(bucket)
                .objectKey(objectKey)
                .status(S3CleanupStatus.PENDING)
                .retryCount(0)
                .nextRetryAt(now)
                .build();
    }

    public void recordFailure(String errorMessage, LocalDateTime failedAt) {
        retryCount += 1;
        lastError = truncate(errorMessage);

        if (retryCount >= MAX_ATTEMPTS) {
            status = S3CleanupStatus.FAILED;
            nextRetryAt = null;
            return;
        }

        nextRetryAt = failedAt.plusMinutes(retryDelayMinutes(retryCount));
    }

    private long retryDelayMinutes(int failureCount) {
        return switch (failureCount) {
            case 1 -> 1;
            case 2 -> 5;
            case 3 -> 30;
            case 4 -> 120;
            default -> throw new IllegalArgumentException("지원하지 않는 S3 정리 재시도 횟수입니다.");
        };
    }

    private String truncate(String value) {
        if (value == null || value.length() <= MAX_ERROR_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_ERROR_LENGTH);
    }
}
