package com.boki.backend.domain.trade.service;

import com.boki.backend.domain.trade.dto.request.TradeManualCreateRequest;
import com.boki.backend.domain.trade.dto.request.TradeUpdateRequest;
import com.boki.backend.domain.trade.dto.response.TradeCalendarDayResponse;
import com.boki.backend.domain.trade.dto.response.TradeCalendarResponse;
import com.boki.backend.domain.trade.dto.response.TradeResponse;
import com.boki.backend.domain.trade.entity.Trade;
import com.boki.backend.domain.trade.entity.TradeInputType;
import com.boki.backend.domain.trade.entity.TradeType;
import com.boki.backend.domain.trade.exception.TradeErrorCode;
import com.boki.backend.domain.trade.repository.TradeRepository;
import com.boki.backend.global.apiPayload.code.GeneralErrorCode;
import com.boki.backend.global.apiPayload.exception.GeneralException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
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
    public List<TradeResponse> getTrades(Long memberId, LocalDate date) {
        List<Trade> trades = date == null
                ? tradeRepository.findAllByMemberIdOrderByTradedAtDescTradeIdDesc(memberId)
                : findTradesInRange(memberId, date.atStartOfDay(), date.plusDays(1).atStartOfDay());

        return trades.stream()
                .map(TradeResponse::from)
                .toList();
    }

    @Override
    public TradeCalendarResponse getTradeCalendar(Long memberId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        List<Trade> trades = findTradesInRange(
                memberId,
                yearMonth.atDay(1).atStartOfDay(),
                yearMonth.plusMonths(1).atDay(1).atStartOfDay()
        );

        Map<LocalDate, List<Trade>> tradesByDate = trades.stream()
                .collect(Collectors.groupingBy(
                        trade -> trade.getTradedAt().toLocalDate(),
                        TreeMap::new,
                        Collectors.toList()
                ));

        List<TradeCalendarDayResponse> days = tradesByDate.entrySet().stream()
                .map(entry -> toCalendarDayResponse(entry.getKey(), entry.getValue()))
                .toList();

        return new TradeCalendarResponse(year, month, days);
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

    private List<Trade> findTradesInRange(Long memberId, LocalDateTime startInclusive, LocalDateTime endExclusive) {
        return tradeRepository.findAllByMemberIdAndTradedAtGreaterThanEqualAndTradedAtLessThanOrderByTradedAtDescTradeIdDesc(
                memberId,
                startInclusive,
                endExclusive
        );
    }

    private TradeCalendarDayResponse toCalendarDayResponse(LocalDate date, List<Trade> trades) {
        long buyCount = countByTradeType(trades, TradeType.BUY);
        long sellCount = countByTradeType(trades, TradeType.SELL);
        return new TradeCalendarDayResponse(date, true, trades.size(), buyCount, sellCount);
    }

    private long countByTradeType(List<Trade> trades, TradeType tradeType) {
        return trades.stream()
                .filter(trade -> trade.getTradeType() == tradeType)
                .count();
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
