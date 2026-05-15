package com.boki.backend.domain.trade.service;

import com.boki.backend.domain.trade.dto.request.TradeManualCreateRequest;
import com.boki.backend.domain.trade.dto.request.TradeUpdateRequest;
import com.boki.backend.domain.trade.dto.response.TradeResponse;
import java.util.List;

public interface TradeService {

    List<TradeResponse> getTrades();

    TradeResponse getTrade(Long tradeId);

    TradeResponse createManualTrade(TradeManualCreateRequest request);

    TradeResponse updateTrade(Long tradeId, TradeUpdateRequest request);

    void deleteTrade(Long tradeId);
}
