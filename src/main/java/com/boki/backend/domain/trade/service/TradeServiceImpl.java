package com.boki.backend.domain.trade.service;

import com.boki.backend.domain.ai.entity.Grade;
import com.boki.backend.domain.ai.repository.AiReportRepository;
import com.boki.backend.domain.review.entity.TradeReview;
import com.boki.backend.domain.review.repository.TradeReviewRepository;
import com.boki.backend.domain.trade.dto.request.TradeManualCreateRequest;
import com.boki.backend.domain.trade.dto.request.ReviewStatus;
import com.boki.backend.domain.trade.dto.request.TradeSearchRequest;
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
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
    private final TradeReviewRepository tradeReviewRepository;
    private final AiReportRepository aiReportRepository;

    @Override
    public List<TradeResponse> getTrades(Long memberId, TradeSearchRequest request) {
        List<Trade> trades = request.date() == null
                ? tradeRepository.findAllByMemberIdOrderByTradedAtDescTradeIdDesc(memberId)
                : findTradesInRange(memberId, request.date().atStartOfDay(), request.date().plusDays(1).atStartOfDay());

        if (request.tradeType() != null) {
            trades = trades.stream()
                    .filter(trade -> trade.getTradeType() == request.tradeType())
                    .toList();
        }

        Map<Long, TradeReview> reviewsByTradeId = findReviewsByTradeId(memberId, trades);
        List<Long> tradeIds = trades.stream().map(Trade::getTradeId).toList();
        Map<Long, Grade> gradesByTradeId = aiReportRepository.findGradeMapByTradeIds(tradeIds);

        return trades.stream()
                .map(trade -> toResponseWithReview(trade, reviewsByTradeId.get(trade.getTradeId()), gradesByTradeId.get(trade.getTradeId())))
                .filter(response -> matchesReviewStatus(response, request.reviewStatus()))
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
        Trade trade = getOwnedTrade(tradeId, memberId);
        TradeReview review = tradeReviewRepository.findByTradeIdAndMemberId(tradeId, memberId)
                .orElse(null);
        Map<Long, Grade> gradesByTradeId = aiReportRepository.findGradeMapByTradeIds(List.of(tradeId));
        return toResponseWithReview(trade, review, gradesByTradeId.get(tradeId));
    }

    @Override
    @Transactional
    public TradeResponse createManualTrade(Long memberId, TradeManualCreateRequest request) {
        BigDecimal quantity = calculateQuantity(request.totalAmount(), request.price());
        Trade trade = Trade.builder()
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
                request.tradeType(),
                normalizeNullableText(request.coinType()),
                price,
                quantity,
                totalAmount,
                request.tradedAt()
        );

        TradeReview review = tradeReviewRepository.findByTradeIdAndMemberId(tradeId, memberId)
                .orElse(null);
        Map<Long, Grade> gradesByTradeId = aiReportRepository.findGradeMapByTradeIds(List.of(tradeId));
        return toResponseWithReview(trade, review, gradesByTradeId.get(tradeId));
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

    private Map<Long, TradeReview> findReviewsByTradeId(Long memberId, Collection<Trade> trades) {
        List<Long> tradeIds = trades.stream()
                .map(Trade::getTradeId)
                .toList();
        if (tradeIds.isEmpty()) {
            return Map.of();
        }
        return tradeReviewRepository.findAllByMemberIdAndTradeIdIn(memberId, tradeIds).stream()
                .collect(Collectors.toMap(TradeReview::getTradeId, Function.identity()));
    }

    private TradeResponse toResponseWithReview(Trade trade, TradeReview review, Grade grade) {
        if (review == null) {
            return TradeResponse.from(trade, ReviewStatus.NOT_COMPLETED, null, null);
        }
        return TradeResponse.from(trade, ReviewStatus.COMPLETED, review.getReviewId(), grade);
    }

    private boolean matchesReviewStatus(TradeResponse response, ReviewStatus reviewStatus) {
        return reviewStatus == null || response.reviewStatus() == reviewStatus;
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
