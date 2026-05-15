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
import com.boki.backend.global.auth.AuthenticatedUserProvider;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TradeServiceImpl implements TradeService {

    private final TradeRepository tradeRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    @Override
    public List<TradeResponse> getTrades() {
        Long memberId = authenticatedUserProvider.getCurrentUserId();
        return tradeRepository.findAllByMemberIdOrderByTradedAtDescTradeIdDesc(memberId).stream()
                .map(TradeResponse::from)
                .toList();
    }

    @Override
    public TradeResponse getTrade(Long tradeId) {
        Long memberId = authenticatedUserProvider.getCurrentUserId();
        return TradeResponse.from(getOwnedTrade(tradeId, memberId));
    }

    @Override
    @Transactional
    public TradeResponse createManualTrade(TradeManualCreateRequest request) {
        Long memberId = authenticatedUserProvider.getCurrentUserId();
        Trade trade = Trade.builder()
                .ruleSetId(request.ruleSetId())
                .memberId(memberId)
                .tradeType(request.tradeType())
                .inputType(TradeInputType.MANUAL)
                .coinType(normalizeRequiredText(request.coinType()))
                .price(request.price())
                .quantity(request.quantity())
                .tradedAt(request.tradedAt())
                .build();

        return TradeResponse.from(tradeRepository.save(trade));
    }

    @Override
    @Transactional
    public TradeResponse updateTrade(Long tradeId, TradeUpdateRequest request) {
        Long memberId = authenticatedUserProvider.getCurrentUserId();
        Trade trade = getOwnedTrade(tradeId, memberId);
        trade.update(
                request.ruleSetId(),
                request.tradeType(),
                normalizeNullableText(request.coinType()),
                request.price(),
                request.quantity(),
                request.tradedAt()
        );

        return TradeResponse.from(trade);
    }

    @Override
    @Transactional
    public void deleteTrade(Long tradeId) {
        Long memberId = authenticatedUserProvider.getCurrentUserId();
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
}
