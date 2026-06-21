package com.boki.backend.global.storage.repository;

import com.boki.backend.global.storage.entity.S3CleanupTask;
import com.boki.backend.global.storage.entity.S3CleanupStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface S3CleanupTaskRepository extends JpaRepository<S3CleanupTask, Long> {

    boolean existsByBucketAndObjectKey(String bucket, String objectKey);

    List<S3CleanupTask> findTop50ByStatusAndNextRetryAtLessThanEqualOrderByCleanupTaskIdAsc(
            S3CleanupStatus status,
            LocalDateTime now
    );
}
