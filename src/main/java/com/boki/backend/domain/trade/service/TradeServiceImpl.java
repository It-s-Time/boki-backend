package com.boki.backend.domain.trade.service;

import com.boki.backend.domain.trade.dto.request.TradeManualCreateRequest;
import com.boki.backend.domain.trade.dto.request.TradeUpdateRequest;
import com.boki.backend.domain.trade.dto.response.TradeResponse;
import com.boki.backend.domain.trade.entity.Trade;
import com.boki.backend.domain.trade.entity.TradeInputType;
import com.boki.backend.domain.trade.exception.TradeErrorCode;
import com.boki.backend.domain.trade.repository.TradeRepository;
import com.boki.backend.global.apiPayload.code.GeneralErrorCode;
import com.boki.backend.global.apiPayload.exception.GeneralException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TradeServiceImpl implements TradeService {

    private static final int QUANTITY_SCALE = 8;

    private final TradeRepository tradeRepository;

    @Override
    public List<TradeResponse> getTrades(Long memberId) {
        return tradeRepository.findAllByMemberIdOrderByTradedAtDescTradeIdDesc(memberId).stream()
                .map(TradeResponse::from)
                .toList();
    }

    @Override
    public TradeResponse getTrade(Long memberId, Long tradeId) {
        return TradeResponse.from(getOwnedTrade(tradeId, memberId));
    }

    @Override
    @Transactional
    public TradeResponse createManualTrade(Long memberId, TradeManualCreateRequest request) {
        BigDecimal quantity = calculateQuantity(request.totalAmount(), request.price());
        Trade trade = Trade.builder()
                .ruleSetId(request.ruleSetId())
                .memberId(memberId)
                .tradeType(request.tradeType())
                .inputType(TradeInputType.MANUAL)
                .coinType(normalizeRequiredText(request.coinType()))
                .price(request.price())
                .quantity(quantity)
                .totalAmount(request.totalAmount())
                .tradedAt(request.tradedAt())
                .build();

        return TradeResponse.from(tradeRepository.save(trade));
    }

    @Override
    @Transactional
    public TradeResponse updateTrade(Long memberId, Long tradeId, TradeUpdateRequest request) {
        Trade trade = getOwnedTrade(tradeId, memberId);
        BigDecimal price = request.price() != null ? request.price() : trade.getPrice();
        BigDecimal totalAmount = request.totalAmount() != null ? request.totalAmount() : trade.getTotalAmount();
        BigDecimal quantity = calculateQuantity(totalAmount, price);

        trade.update(
                request.ruleSetId(),
                request.tradeType(),
                normalizeNullableText(request.coinType()),
                price,
                quantity,
                totalAmount,
                request.tradedAt()
        );

        return TradeResponse.from(trade);
    }

    @Override
    @Transactional
    public void deleteTrade(Long memberId, Long tradeId) {
        Trade trade = getOwnedTrade(tradeId, memberId);
        tradeRepository.delete(trade);
    }

    //private method

    private Trade getOwnedTrade(Long tradeId, Long memberId) {
        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new GeneralException(TradeErrorCode.TRADE_NOT_FOUND));
        if (!trade.getMemberId().equals(memberId)) {
            throw new GeneralException(TradeErrorCode.TRADE_FORBIDDEN);
        }
        return trade;
    }

    private String normalizeRequiredText(String value) {
        String normalized = normalizeNullableText(value);
        if (normalized == null) {
            throw new GeneralException(GeneralErrorCode.BAD_REQUEST);
        }
        return normalized;
    }

    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        if (normalized.isBlank()) {
            throw new GeneralException(GeneralErrorCode.BAD_REQUEST);
        }
        return normalized;
    }

    private BigDecimal calculateQuantity(BigDecimal totalAmount, BigDecimal price) {
        if (totalAmount == null || price == null
                || totalAmount.compareTo(BigDecimal.ZERO) <= 0
                || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new GeneralException(GeneralErrorCode.BAD_REQUEST);
        }
        return totalAmount.divide(price, QUANTITY_SCALE, RoundingMode.HALF_UP);
    }
}
