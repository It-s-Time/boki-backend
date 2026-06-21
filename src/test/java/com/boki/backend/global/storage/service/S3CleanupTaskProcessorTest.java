package com.boki.backend.global.storage.service;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.boki.backend.domain.review.service.ReviewImageStorage;
import com.boki.backend.global.storage.entity.S3CleanupStatus;
import com.boki.backend.global.storage.entity.S3CleanupTask;
import com.boki.backend.global.storage.repository.S3CleanupTaskRepository;
import java.time.LocalDateTime;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ActiveProfiles("test")
@SpringBootTest
class S3CleanupTaskProcessorTest {

    @Autowired
    private S3CleanupTaskRepository cleanupTaskRepository;

    @Autowired
    private S3CleanupTaskProcessor cleanupTaskProcessor;

    @MockitoBean
    private ReviewImageStorage reviewImageStorage;

    @BeforeEach
    void setUp() {
        cleanupTaskRepository.deleteAll();
    }

    @Test
    void successfulS3DeletionRemovesCleanupTask() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 19, 12, 0);
        cleanupTaskRepository.save(S3CleanupTask.pending("test-bucket", "reviews/1/1/image.png", now));

        cleanupTaskProcessor.processDueTasks(now);

        verify(reviewImageStorage).delete("test-bucket", "reviews/1/1/image.png");
        Assertions.assertThat(cleanupTaskRepository.findAll()).isEmpty();
    }

    @Test
    void firstFailureSchedulesRetryAfterOneMinute() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 19, 12, 0);
        cleanupTaskRepository.save(S3CleanupTask.pending("test-bucket", "reviews/1/1/image.png", now));
        doThrow(new RuntimeException("temporary S3 failure"))
                .when(reviewImageStorage)
                .delete("test-bucket", "reviews/1/1/image.png");

        cleanupTaskProcessor.processDueTasks(now);

        Assertions.assertThat(cleanupTaskRepository.findAll())
                .singleElement()
                .satisfies(task -> {
                    Assertions.assertThat(task.getStatus()).isEqualTo(S3CleanupStatus.PENDING);
                    Assertions.assertThat(task.getRetryCount()).isEqualTo(1);
                    Assertions.assertThat(task.getNextRetryAt()).isEqualTo(now.plusMinutes(1));
                    Assertions.assertThat(task.getLastError()).contains("temporary S3 failure");
                });
    }

    @Test
    void fifthFailureMarksCleanupTaskAsFailed() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 19, 12, 0);
        cleanupTaskRepository.save(S3CleanupTask.builder()
                .bucket("test-bucket")
                .objectKey("reviews/1/1/image.png")
                .status(S3CleanupStatus.PENDING)
                .retryCount(4)
                .nextRetryAt(now)
                .build());
        doThrow(new RuntimeException("permanent S3 failure"))
                .when(reviewImageStorage)
                .delete("test-bucket", "reviews/1/1/image.png");

        cleanupTaskProcessor.processDueTasks(now);

        Assertions.assertThat(cleanupTaskRepository.findAll())
                .singleElement()
                .satisfies(task -> {
                    Assertions.assertThat(task.getStatus()).isEqualTo(S3CleanupStatus.FAILED);
                    Assertions.assertThat(task.getRetryCount()).isEqualTo(5);
                    Assertions.assertThat(task.getNextRetryAt()).isNull();
                    Assertions.assertThat(task.getLastError()).contains("permanent S3 failure");
                });
    }
}
