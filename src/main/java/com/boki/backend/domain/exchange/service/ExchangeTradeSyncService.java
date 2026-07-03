package com.boki.backend.domain.exchange.service;

import com.boki.backend.domain.exchange.dto.response.ExchangeTradeSyncResponse;

public interface ExchangeTradeSyncService {

    ExchangeTradeSyncResponse syncCurrentUserTrades(Long memberId);

    ExchangeTradeSyncResponse syncApiKeyTrades(Long memberApiKeyId);
}
