package com.boki.backend.domain.exchange.service;

import com.boki.backend.domain.trade.dto.response.TradeResponse;
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
import com.boki.backend.global.auth.AuthenticatedUserProvider;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

    private final ApiKeyRepository apiKeyRepository;
    private final TradeRepository tradeRepository;
    private final UpbitClient upbitClient;
    private final SecretKeyEncryptor secretKeyEncryptor;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    @Override
    @Transactional
    public ExchangeTradeSyncResponse syncCurrentUserTrades() {
        Long memberId = authenticatedUserProvider.getCurrentUserId();
        ApiKey credential = apiKeyRepository.findByMemberId(memberId)
                .orElseThrow(() -> new GeneralException(ExchangeErrorCode.CREDENTIAL_NOT_FOUND));

        String secretKey = secretKeyEncryptor.decrypt(credential.getSecretKey());
        List<UpbitClosedOrderResponse> closedOrders =
                upbitClient.getClosedOrders(credential.getAccessKey(), secretKey);

        int skippedCount = 0;
        List<Trade> tradesToSave = new ArrayList<>();
        for (UpbitClosedOrderResponse order : closedOrders) {
            if (tradeRepository.existsByMemberIdAndExternalTradeId(memberId, order.uuid())) {
                skippedCount++;
                continue;
            }

            tradesToSave.add(Trade.builder()
                    .ruleSetId(null)
                    .memberId(memberId)
                    .tradeType(toTradeType(order.side()))
                    .inputType(TradeInputType.API)
                    .coinType(order.market())
                    .price(resolvePrice(order))
                    .quantity(resolveQuantity(order))
                    .tradedAt(resolveTradedAt(order))
                    .externalTradeId(order.uuid())
                    .build());
        }

        List<TradeResponse> syncedTrades = tradeRepository.saveAll(tradesToSave).stream()
                .map(TradeResponse::from)
                .toList();

        return new ExchangeTradeSyncResponse(syncedTrades.size(), skippedCount, syncedTrades);
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

    private LocalDateTime resolveTradedAt(UpbitClosedOrderResponse order) {
        if (order.createdAt() == null) {
            throw new GeneralException(ExchangeErrorCode.API_REQUEST_FAILED);
        }
        return order.createdAt().toLocalDateTime();
    }
}
