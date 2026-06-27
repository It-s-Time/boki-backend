package com.boki.backend.domain.exchange.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExchangeTradeSyncAsyncExecutor {

    private final ExchangeTradeSyncService exchangeTradeSyncService;

    @Async("exchangeTradeSyncTaskExecutor")
    public void syncApiKeyTrades(Long memberApiKeyId) {
        exchangeTradeSyncService.syncApiKeyTrades(memberApiKeyId);
    }
}
