package com.boki.backend.global.storage.service;

import com.boki.backend.global.config.S3Properties;
import com.boki.backend.global.storage.entity.S3CleanupTask;
import com.boki.backend.global.storage.entity.S3CleanupStatus;
import com.boki.backend.global.storage.repository.S3CleanupTaskRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class S3CleanupTaskService {

    private final S3CleanupTaskRepository cleanupTaskRepository;
    private final S3Properties s3Properties;

    @Transactional
    public void enqueueAll(List<String> objectKeys) {
        enqueue(objectKeys);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enqueueAllInNewTransaction(List<String> objectKeys) {
        enqueue(objectKeys);
    }

    private void enqueue(List<String> objectKeys) {
        LocalDateTime now = LocalDateTime.now();
        List<S3CleanupTask> tasks = objectKeys.stream()
                .distinct()
                .filter(objectKey -> !cleanupTaskRepository.existsByBucketAndObjectKey(
                        s3Properties.bucket(),
                        objectKey
                ))
                .map(objectKey -> S3CleanupTask.pending(s3Properties.bucket(), objectKey, now))
                .toList();
        cleanupTaskRepository.saveAll(tasks);
    }

    @Transactional(readOnly = true)
    public List<S3CleanupTask> findDueTasks(LocalDateTime now) {
        return cleanupTaskRepository
                .findTop50ByStatusAndNextRetryAtLessThanEqualOrderByCleanupTaskIdAsc(
                        S3CleanupStatus.PENDING,
                        now
                );
    }

    @Transactional
    public void complete(Long cleanupTaskId) {
        cleanupTaskRepository.findById(cleanupTaskId)
                .ifPresent(cleanupTaskRepository::delete);
    }

    @Transactional
    public void recordFailure(Long cleanupTaskId, String errorMessage, LocalDateTime failedAt) {
        cleanupTaskRepository.findById(cleanupTaskId)
                .ifPresent(task -> task.recordFailure(errorMessage, failedAt));
    }
}
