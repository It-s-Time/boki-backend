package com.boki.backend.domain.exchange.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.boki.backend.domain.exchange.client.UpbitClient;
import com.boki.backend.domain.exchange.client.UpbitClosedOrderResponse;
import com.boki.backend.domain.exchange.entity.ApiKey;
import com.boki.backend.domain.exchange.exception.ExchangeErrorCode;
import com.boki.backend.domain.exchange.repository.ApiKeyRepository;
import com.boki.backend.domain.exchange.util.SecretKeyEncryptor;
import com.boki.backend.domain.trade.repository.TradeRepository;
import com.boki.backend.global.apiPayload.exception.GeneralException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ActiveProfiles("test")
@SpringBootTest
class ExchangeTradeSyncServiceImplTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 27, 10, 15);

    @Autowired
    private ExchangeTradeSyncService exchangeTradeSyncService;

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
    }

    @Test
    void syncCurrentUserTradesSplitsRecentThirtyDaysIntoSevenDayRangesAndUpdatesMetadata() {
        apiKeyRepository.save(ApiKey.builder()
                .memberId(1L)
                .accessKey("access-key")
                .secretKey(secretKeyEncryptor.encrypt("secret-key"))
                .build());
        AtomicBoolean returnedOrder = new AtomicBoolean(false);
        when(upbitClient.getClosedOrders(eq("access-key"), eq("secret-key"), any(), any()))
                .thenAnswer(invocation -> {
                    LocalDateTime startTime = invocation.getArgument(2);
                    LocalDateTime endTime = invocation.getArgument(3);
                    assertThat(Duration.between(startTime, endTime)).isLessThanOrEqualTo(Duration.ofDays(7));
                    if (returnedOrder.compareAndSet(false, true)) {
                        return List.of(closedOrder("upbit-order-uuid"));
                    }
                    return List.of();
                });

        var response = exchangeTradeSyncService.syncCurrentUserTrades(1L);

        assertThat(response.syncedCount()).isEqualTo(1);
        assertThat(response.skippedCount()).isZero();
        assertThat(tradeRepository.findAll()).hasSize(1);
        assertThat(tradeRepository.findAll().get(0).getTotalAmount()).isEqualByComparingTo("4999999.21020");
        ApiKey credential = apiKeyRepository.findByMemberId(1L).orElseThrow();
        assertThat(credential.getLastTradeSyncedAt()).isEqualTo(NOW);
        assertThat(credential.getNextTradeSyncAt()).isEqualTo(NOW.plusHours(6));
        assertThat(credential.getTradeSyncStartedAt()).isNull();

        ArgumentCaptor<LocalDateTime> startCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(upbitClient, org.mockito.Mockito.atLeast(5))
                .getClosedOrders(eq("access-key"), eq("secret-key"), startCaptor.capture(), endCaptor.capture());
        assertThat(startCaptor.getAllValues()).first().isEqualTo(LocalDateTime.of(2026, 5, 28, 0, 0));
        assertThat(endCaptor.getAllValues()).last().isEqualTo(NOW);
    }

    @Test
    void syncCurrentUserTradesSkipsAlreadyStoredExternalTradeIds() {
        apiKeyRepository.save(ApiKey.builder()
                .memberId(1L)
                .accessKey("access-key")
                .secretKey(secretKeyEncryptor.encrypt("secret-key"))
                .build());
        when(upbitClient.getClosedOrders(eq("access-key"), eq("secret-key"), any(), any()))
                .thenAnswer(invocation -> {
                    LocalDateTime startTime = invocation.getArgument(2);
                    if (startTime.equals(LocalDateTime.of(2026, 5, 28, 0, 0))) {
                        return List.of(closedOrder("duplicate-order-uuid"));
                    }
                    return List.of();
                });

        var firstResponse = exchangeTradeSyncService.syncCurrentUserTrades(1L);
        var secondResponse = exchangeTradeSyncService.syncCurrentUserTrades(1L);

        assertThat(firstResponse.syncedCount()).isEqualTo(1);
        assertThat(secondResponse.syncedCount()).isZero();
        assertThat(secondResponse.skippedCount()).isEqualTo(1);
        assertThat(tradeRepository.findAll()).hasSize(1);
    }

    @Test
    void syncFailureClearsStartedAtAndSchedulesRetry() {
        apiKeyRepository.save(ApiKey.builder()
                .memberId(1L)
                .accessKey("access-key")
                .secretKey(secretKeyEncryptor.encrypt("secret-key"))
                .build());
        when(upbitClient.getClosedOrders(eq("access-key"), eq("secret-key"), any(), any()))
                .thenThrow(new GeneralException(ExchangeErrorCode.API_REQUEST_FAILED));

        assertThatThrownBy(() -> exchangeTradeSyncService.syncCurrentUserTrades(1L))
                .isInstanceOf(GeneralException.class);

        ApiKey credential = apiKeyRepository.findByMemberId(1L).orElseThrow();
        assertThat(credential.getLastTradeSyncedAt()).isNull();
        assertThat(credential.getNextTradeSyncAt()).isEqualTo(NOW.plusMinutes(10));
        assertThat(credential.getTradeSyncStartedAt()).isNull();
    }

    private UpbitClosedOrderResponse closedOrder(String uuid) {
        return new UpbitClosedOrderResponse(
                uuid,
                "bid",
                "KRW-BTC",
                new BigDecimal("106038000"),
                new BigDecimal("0.04715290"),
                new BigDecimal("0.04715290"),
                new BigDecimal("4999999.98220000"),
                OffsetDateTime.parse("2026-06-10T14:35:10+09:00")
        );
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
