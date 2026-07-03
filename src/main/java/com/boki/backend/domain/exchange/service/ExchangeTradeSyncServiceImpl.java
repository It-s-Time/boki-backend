package com.boki.backend.domain.exchange.service;

import com.boki.backend.domain.trade.dto.response.TradeResponse;
import com.boki.backend.domain.exchange.config.UpbitTradeSyncProperties;
import com.boki.backend.domain.exchange.dto.response.ExchangeTradeSyncResponse;
import com.boki.backend.domain.trade.entity.Trade;
import com.boki.backend.domain.trade.entity.TradeInputType;
import com.boki.backend.domain.trade.entity.TradeType;
import com.boki.backend.domain.trade.repository.TradeRepository;
import com.boki.backend.domain.exchange.client.UpbitClient;
import com.boki.backend.domain.exchange.client.UpbitClosedOrderResponse;
import com.boki.backend.domain.exchange.entity.ApiKey;
import com.boki.backend.domain.exchange.exception.ExchangeErrorCode;
import com.boki.backend.domain.exchange.repository.ApiKeyRepository;
import com.boki.backend.domain.exchange.util.SecretKeyEncryptor;
import com.boki.backend.global.apiPayload.exception.GeneralException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExchangeTradeSyncServiceImpl implements ExchangeTradeSyncService {

    private static final int UPBIT_CLOSED_ORDER_LIMIT = 1000;
    private static final int UPBIT_MAX_RANGE_DAYS = 7;
    private static final int MAX_SPLIT_DEPTH = 12;

    private final ApiKeyRepository apiKeyRepository;
    private final TradeRepository tradeRepository;
    private final UpbitClient upbitClient;
    private final SecretKeyEncryptor secretKeyEncryptor;
    private final Clock clock;
    private final UpbitTradeSyncProperties syncProperties;

    @Override
    @Transactional(noRollbackFor = GeneralException.class)
    public ExchangeTradeSyncResponse syncCurrentUserTrades(Long memberId) {
        ApiKey credential = apiKeyRepository.findByMemberId(memberId)
                .orElseThrow(() -> new GeneralException(ExchangeErrorCode.CREDENTIAL_NOT_FOUND));

        return syncCredential(credential);
    }

    @Override
    @Transactional(noRollbackFor = GeneralException.class)
    public ExchangeTradeSyncResponse syncApiKeyTrades(Long memberApiKeyId) {
        ApiKey credential = apiKeyRepository.findById(memberApiKeyId)
                .orElseThrow(() -> new GeneralException(ExchangeErrorCode.CREDENTIAL_NOT_FOUND));

        return syncCredential(credential);
    }

    private ExchangeTradeSyncResponse syncCredential(ApiKey credential) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (!claimTradeSync(credential, now)) {
            return new ExchangeTradeSyncResponse(0, 0, List.of());
        }

        try {
            ExchangeTradeSyncResponse response = fetchAndSaveTrades(credential);
            credential.completeTradeSync(now, syncProperties.intervalHours());
            return response;
        } catch (GeneralException exception) {
            credential.failTradeSync(now.plusMinutes(syncProperties.retryDelayMinutes()));
            throw exception;
        }
    }

    private boolean claimTradeSync(ApiKey credential, LocalDateTime now) {
        LocalDateTime staleStartedBefore = now.minusMinutes(syncProperties.staleLockMinutes());
        int updatedCount = apiKeyRepository.claimTradeSync(
                credential.getMemberApiKeyId(),
                now,
                staleStartedBefore
        );
        if (updatedCount == 0) {
            return false;
        }
        credential.startTradeSync(now);
        return true;
    }

    private ExchangeTradeSyncResponse fetchAndSaveTrades(ApiKey credential) {
        Long memberId = credential.getMemberId();
        String secretKey = secretKeyEncryptor.decrypt(credential.getSecretKey());
        List<UpbitClosedOrderResponse> closedOrders = fetchRecentClosedOrders(
                credential.getAccessKey(),
                secretKey
        );

        int skippedCount = 0;
        List<Trade> tradesToSave = new ArrayList<>();
        for (UpbitClosedOrderResponse order : closedOrders) {
            if (tradeRepository.existsByMemberIdAndExternalTradeId(memberId, order.uuid())) {
                skippedCount++;
                continue;
            }

            BigDecimal price = resolvePrice(order);
            BigDecimal quantity = resolveQuantity(order);
            tradesToSave.add(Trade.builder()
                    .ruleSetId(null)
                    .memberId(memberId)
                    .tradeType(toTradeType(order.side()))
                    .inputType(TradeInputType.API)
                    .coinType(order.market())
                    .price(price)
                    .quantity(quantity)
                    .totalAmount(calculateTotalAmount(price, quantity))
                    .tradedAt(resolveTradedAt(order))
                    .externalTradeId(order.uuid())
                    .build());
        }

        List<TradeResponse> syncedTrades = tradeRepository.saveAll(tradesToSave).stream()
                .map(TradeResponse::from)
                .toList();

        return new ExchangeTradeSyncResponse(syncedTrades.size(), skippedCount, syncedTrades);
    }

    private List<UpbitClosedOrderResponse> fetchRecentClosedOrders(String accessKey, String secretKey) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime startTime = LocalDate.now(clock).minusDays(30).atStartOfDay();

        List<UpbitClosedOrderResponse> closedOrders = new ArrayList<>();
        LocalDateTime segmentStart = startTime;
        while (segmentStart.isBefore(now)) {
            LocalDateTime segmentEnd = min(segmentStart.plusDays(UPBIT_MAX_RANGE_DAYS), now);
            closedOrders.addAll(fetchClosedOrdersInRange(accessKey, secretKey, segmentStart, segmentEnd, 0));
            segmentStart = segmentEnd;
        }
        return closedOrders;
    }

    private List<UpbitClosedOrderResponse> fetchClosedOrdersInRange(
            String accessKey,
            String secretKey,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int splitDepth
    ) {
        List<UpbitClosedOrderResponse> closedOrders =
                upbitClient.getClosedOrders(accessKey, secretKey, startTime, endTime);

        if (closedOrders.size() < UPBIT_CLOSED_ORDER_LIMIT || !canSplit(startTime, endTime, splitDepth)) {
            return closedOrders;
        }

        LocalDateTime midpoint = startTime.plus(Duration.between(startTime, endTime).dividedBy(2));
        List<UpbitClosedOrderResponse> splitOrders = new ArrayList<>();
        splitOrders.addAll(fetchClosedOrdersInRange(accessKey, secretKey, startTime, midpoint, splitDepth + 1));
        splitOrders.addAll(fetchClosedOrdersInRange(accessKey, secretKey, midpoint, endTime, splitDepth + 1));
        return splitOrders;
    }

    private boolean canSplit(LocalDateTime startTime, LocalDateTime endTime, int splitDepth) {
        if (splitDepth >= MAX_SPLIT_DEPTH) {
            return false;
        }
        LocalDateTime midpoint = startTime.plus(Duration.between(startTime, endTime).dividedBy(2));
        return midpoint.isAfter(startTime) && midpoint.isBefore(endTime);
    }

    private LocalDateTime min(LocalDateTime first, LocalDateTime second) {
        if (first.isBefore(second)) {
            return first;
        }
        return second;
    }

    private TradeType toTradeType(String side) {
        if ("bid".equalsIgnoreCase(side)) {
            return TradeType.BUY;
        }
        if ("ask".equalsIgnoreCase(side)) {
            return TradeType.SELL;
        }
        throw new GeneralException(ExchangeErrorCode.API_REQUEST_FAILED);
    }

    private BigDecimal resolvePrice(UpbitClosedOrderResponse order) {
        if (order.price() != null && order.price().compareTo(BigDecimal.ZERO) > 0) {
            return order.price();
        }
        BigDecimal executedFunds = order.executedFunds();
        BigDecimal executedVolume = resolveQuantity(order);
        if (executedFunds == null || executedVolume.compareTo(BigDecimal.ZERO) <= 0) {
            throw new GeneralException(ExchangeErrorCode.API_REQUEST_FAILED);
        }
        return executedFunds.divide(executedVolume, 5, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveQuantity(UpbitClosedOrderResponse order) {
        if (order.executedVolume() != null && order.executedVolume().compareTo(BigDecimal.ZERO) > 0) {
            return order.executedVolume();
        }
        if (order.volume() != null && order.volume().compareTo(BigDecimal.ZERO) > 0) {
            return order.volume();
        }
        throw new GeneralException(ExchangeErrorCode.API_REQUEST_FAILED);
    }

    private BigDecimal calculateTotalAmount(BigDecimal price, BigDecimal quantity) {
        return price.multiply(quantity).setScale(5, RoundingMode.HALF_UP);
    }

    private LocalDateTime resolveTradedAt(UpbitClosedOrderResponse order) {
        if (order.createdAt() == null) {
            throw new GeneralException(ExchangeErrorCode.API_REQUEST_FAILED);
        }
        return order.createdAt().toLocalDateTime();
    }
}
