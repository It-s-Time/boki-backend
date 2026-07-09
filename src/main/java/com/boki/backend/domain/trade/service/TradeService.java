package com.boki.backend.domain.trade.service;

import com.boki.backend.domain.trade.dto.request.TradeManualCreateRequest;
import com.boki.backend.domain.trade.dto.request.TradeSearchRequest;
import com.boki.backend.domain.trade.dto.request.TradeUpdateRequest;
import com.boki.backend.domain.trade.dto.response.TradeCalendarResponse;
import com.boki.backend.domain.trade.dto.response.TradeResponse;
import java.util.List;

public interface TradeService {

    List<TradeResponse> getTrades(Long memberId, TradeSearchRequest request);

    TradeCalendarResponse getTradeCalendar(Long memberId, int year, int month);

    TradeResponse getTrade(Long memberId, Long tradeId);

    TradeResponse createManualTrade(Long memberId, TradeManualCreateRequest request);

    TradeResponse updateTrade(Long memberId, Long tradeId, TradeUpdateRequest request);

    void deleteTrade(Long memberId, Long tradeId);
}
