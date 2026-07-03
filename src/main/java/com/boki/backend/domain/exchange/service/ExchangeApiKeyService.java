package com.boki.backend.domain.exchange.service;

import com.boki.backend.domain.exchange.dto.request.ApiKeySaveRequest;
import com.boki.backend.domain.exchange.dto.response.ApiKeySaveResponse;

public interface ExchangeApiKeyService {

    ApiKeySaveResponse saveCredential(Long memberId, ApiKeySaveRequest request);
}
