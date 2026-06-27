package com.boki.backend.domain.exchange.service;

import com.boki.backend.domain.exchange.config.UpbitTradeSyncProperties;
import com.boki.backend.domain.exchange.dto.response.ExchangeTradeSyncResponse;
import com.boki.backend.domain.exchange.entity.ApiKey;
import com.boki.backend.domain.exchange.repository.ApiKeyRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeTradeSyncBatchProcessor {

    private final ApiKeyRepository apiKeyRepository;
    private final ExchangeTradeSyncService exchangeTradeSyncService;
    private final UpbitTradeSyncProperties syncProperties;

    public void processDueSyncs(LocalDateTime now) {
        LocalDateTime staleStartedBefore = now.minusMinutes(syncProperties.staleLockMinutes());
        for (ApiKey credential : apiKeyRepository.findDueTradeSyncTargets(
                now,
                staleStartedBefore,
                PageRequest.of(0, syncProperties.batchSize())
        )) {
            processCredential(credential);
        }
    }

    private void processCredential(ApiKey credential) {
        try {
            ExchangeTradeSyncResponse response = exchangeTradeSyncService.syncApiKeyTrades(
                    credential.getMemberApiKeyId()
            );
            log.info(
                    "Exchange trade sync batch completed. memberApiKeyId={}, memberId={}, syncedCount={}, skippedCount={}",
                    credential.getMemberApiKeyId(),
                    credential.getMemberId(),
                    response.syncedCount(),
                    response.skippedCount()
            );
        } catch (RuntimeException exception) {
            log.warn(
                    "Exchange trade sync batch failed. memberApiKeyId={}, memberId={}",
                    credential.getMemberApiKeyId(),
                    credential.getMemberId(),
                    exception
            );
        }
    }
}
