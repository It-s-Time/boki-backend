package com.boki.backend.domain.exchange.scheduler;

import com.boki.backend.domain.exchange.service.ExchangeTradeSyncBatchProcessor;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "boki.exchange.upbit.sync", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class ExchangeTradeSyncScheduler {

    private final ExchangeTradeSyncBatchProcessor exchangeTradeSyncBatchProcessor;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${boki.exchange.upbit.sync.fixed-delay-ms:60000}")
    public void runTradeSync() {
        exchangeTradeSyncBatchProcessor.processDueSyncs(LocalDateTime.now(clock));
    }
}
