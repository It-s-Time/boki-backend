package com.boki.backend.global.storage.service;

import com.boki.backend.global.storage.entity.S3CleanupTask;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3CleanupTaskProcessor {

    private final S3CleanupTaskService cleanupTaskService;
    private final S3ObjectStorage s3ObjectStorage;

    public void processDueTasks(LocalDateTime now) {
        cleanupTaskService.findDueTasks(now).forEach(task -> processTask(task, now));
    }

    private void processTask(S3CleanupTask task, LocalDateTime now) {
        try {
            s3ObjectStorage.delete(task.getBucket(), task.getObjectKey());
            cleanupTaskService.complete(task.getCleanupTaskId());
        } catch (RuntimeException exception) {
            log.warn(
                    "Failed to process S3 cleanup task. taskId={}, bucket={}, key={}, retryCount={}",
                    task.getCleanupTaskId(),
                    task.getBucket(),
                    task.getObjectKey(),
                    task.getRetryCount(),
                    exception
            );
            cleanupTaskService.recordFailure(
                    task.getCleanupTaskId(),
                    errorMessageOf(exception),
                    now
            );
        }
    }

    private String errorMessageOf(RuntimeException exception) {
        return exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }
}
