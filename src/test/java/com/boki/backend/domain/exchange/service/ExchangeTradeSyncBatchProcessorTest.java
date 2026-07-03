package com.boki.backend.domain.exchange.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.boki.backend.domain.exchange.client.UpbitClient;
import com.boki.backend.domain.exchange.entity.ApiKey;
import com.boki.backend.domain.exchange.repository.ApiKeyRepository;
import com.boki.backend.domain.exchange.util.SecretKeyEncryptor;
import com.boki.backend.domain.trade.repository.TradeRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ActiveProfiles("test")
@SpringBootTest
class ExchangeTradeSyncBatchProcessorTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 27, 10, 15);

    @Autowired
    private ExchangeTradeSyncBatchProcessor exchangeTradeSyncBatchProcessor;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private SecretKeyEncryptor secretKeyEncryptor;

    @MockitoBean
    private UpbitClient upbitClient;

    @BeforeEach
    void setUp() {
        tradeRepository.deleteAll();
        apiKeyRepository.deleteAll();
        when(upbitClient.getClosedOrders(any(), any(), any(), any())).thenReturn(List.of());
    }

    @Test
    void processDueSyncsOnlyRunsDueAndStaleStartedCredentials() {
        ApiKey due = credential(1L, "due-access");
        due.markTradeSyncDue(NOW.minusMinutes(1));
        apiKeyRepository.save(due);

        ApiKey notDue = credential(2L, "not-due-access");
        notDue.scheduleNextTradeSync(NOW.plusMinutes(1));
        apiKeyRepository.save(notDue);

        ApiKey running = credential(3L, "running-access");
        running.markTradeSyncDue(NOW.minusMinutes(1));
        running.startTradeSync(NOW.minusMinutes(5));
        apiKeyRepository.save(running);

        ApiKey stale = credential(4L, "stale-access");
        stale.markTradeSyncDue(NOW.minusMinutes(1));
        stale.startTradeSync(NOW.minusMinutes(31));
        apiKeyRepository.save(stale);

        exchangeTradeSyncBatchProcessor.processDueSyncs(NOW);

        verify(upbitClient, atLeastOnce()).getClosedOrders(eq("due-access"), eq("secret-key"), any(), any());
        verify(upbitClient, atLeastOnce()).getClosedOrders(eq("stale-access"), eq("secret-key"), any(), any());
        verify(upbitClient, never()).getClosedOrders(eq("not-due-access"), any(), any(), any());
        verify(upbitClient, never()).getClosedOrders(eq("running-access"), any(), any(), any());
    }

    private ApiKey credential(Long memberId, String accessKey) {
        return ApiKey.builder()
                .memberId(memberId)
                .accessKey(accessKey)
                .secretKey(secretKeyEncryptor.encrypt("secret-key"))
                .build();
    }

    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-06-27T01:15:00Z"), KST);
        }
    }
}
