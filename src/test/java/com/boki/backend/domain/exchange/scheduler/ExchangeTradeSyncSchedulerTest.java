package com.boki.backend.domain.exchange.scheduler;

import static org.mockito.Mockito.verify;

import com.boki.backend.domain.exchange.service.ExchangeTradeSyncBatchProcessor;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

import org.mockito.Mockito;

class ExchangeTradeSyncSchedulerTest {

    @Test
    void scheduledRunProcessesDueTradeSyncs() {
        ExchangeTradeSyncBatchProcessor processor = Mockito.mock(ExchangeTradeSyncBatchProcessor.class);
        Clock clock = Clock.fixed(Instant.parse("2026-06-27T01:15:00Z"), ZoneId.of("Asia/Seoul"));
        ExchangeTradeSyncScheduler scheduler = new ExchangeTradeSyncScheduler(processor, clock);

        scheduler.runTradeSync();

        verify(processor).processDueSyncs(LocalDateTime.of(2026, 6, 27, 10, 15));
    }
}
