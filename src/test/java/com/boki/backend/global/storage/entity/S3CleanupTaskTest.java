package com.boki.backend.global.storage.entity;

import java.time.LocalDateTime;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class S3CleanupTaskTest {

    @Test
    void failureUsesBackoffSequenceAndStopsAfterFifthAttempt() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 19, 12, 0);
        S3CleanupTask task = S3CleanupTask.pending("test-bucket", "reviews/1/1/image.png", now);

        task.recordFailure("first", now);
        Assertions.assertThat(task.getNextRetryAt()).isEqualTo(now.plusMinutes(1));

        task.recordFailure("second", now);
        Assertions.assertThat(task.getNextRetryAt()).isEqualTo(now.plusMinutes(5));

        task.recordFailure("third", now);
        Assertions.assertThat(task.getNextRetryAt()).isEqualTo(now.plusMinutes(30));

        task.recordFailure("fourth", now);
        Assertions.assertThat(task.getNextRetryAt()).isEqualTo(now.plusHours(2));

        task.recordFailure("fifth", now);
        Assertions.assertThat(task.getStatus()).isEqualTo(S3CleanupStatus.FAILED);
        Assertions.assertThat(task.getRetryCount()).isEqualTo(5);
        Assertions.assertThat(task.getNextRetryAt()).isNull();
        Assertions.assertThat(task.getLastError()).isEqualTo("fifth");
    }
}
