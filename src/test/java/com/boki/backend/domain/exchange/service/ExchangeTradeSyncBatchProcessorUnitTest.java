package com.boki.backend.domain.exchange.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.boki.backend.domain.exchange.config.UpbitTradeSyncProperties;
import com.boki.backend.domain.exchange.entity.ApiKey;
import com.boki.backend.domain.exchange.exception.ExchangeErrorCode;
import com.boki.backend.domain.exchange.repository.ApiKeyRepository;
import com.boki.backend.global.apiPayload.exception.GeneralException;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ExchangeTradeSyncBatchProcessorUnitTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 27, 8, 30);

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private ExchangeTradeSyncService exchangeTradeSyncService;

    private ExchangeTradeSyncBatchProcessor exchangeTradeSyncBatchProcessor;

    @BeforeEach
    void setUp() {
        exchangeTradeSyncBatchProcessor = new ExchangeTradeSyncBatchProcessor(
                apiKeyRepository,
                exchangeTradeSyncService,
                new UpbitTradeSyncProperties(true, 60_000L, 6L, 10L, 30L, 50)
        );
    }

    @Test
    void processDueSyncsContinuesWhenOneCredentialFails() {
        ApiKey failedCredential = credential(1L, 10L);
        ApiKey nextCredential = credential(2L, 20L);
        when(apiKeyRepository.findDueTradeSyncTargets(
                eq(NOW),
                eq(NOW.minusMinutes(30)),
                any(Pageable.class)
        )).thenReturn(List.of(failedCredential, nextCredential));
        when(exchangeTradeSyncService.syncApiKeyTrades(1L))
                .thenThrow(new GeneralException(ExchangeErrorCode.API_REQUEST_FAILED));

        assertDoesNotThrow(() -> exchangeTradeSyncBatchProcessor.processDueSyncs(NOW));

        verify(exchangeTradeSyncService).syncApiKeyTrades(1L);
        verify(exchangeTradeSyncService).syncApiKeyTrades(2L);
    }

    private ApiKey credential(Long memberApiKeyId, Long memberId) {
        ApiKey credential = mock(ApiKey.class);
        when(credential.getMemberApiKeyId()).thenReturn(memberApiKeyId);
        when(credential.getMemberId()).thenReturn(memberId);
        return credential;
    }
}
