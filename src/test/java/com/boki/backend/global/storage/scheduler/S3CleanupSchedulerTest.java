package com.boki.backend.global.storage.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.boki.backend.global.storage.service.S3CleanupTaskProcessor;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class S3CleanupSchedulerTest {

    @Mock
    private S3CleanupTaskProcessor cleanupTaskProcessor;

    @InjectMocks
    private S3CleanupScheduler cleanupScheduler;

    @Test
    void scheduledRunProcessesDueCleanupTasks() {
        cleanupScheduler.runCleanup();

        verify(cleanupTaskProcessor).processDueTasks(any(LocalDateTime.class));
    }
}
