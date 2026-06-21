package com.boki.backend.global.storage.scheduler;

import com.boki.backend.global.storage.service.S3CleanupTaskProcessor;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "boki.aws.s3", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class S3CleanupScheduler {

    private final S3CleanupTaskProcessor cleanupTaskProcessor;

    @Scheduled(fixedDelayString = "${boki.aws.s3.cleanup.fixed-delay-ms:60000}")
    public void runCleanup() {
        cleanupTaskProcessor.processDueTasks(LocalDateTime.now());
    }
}
