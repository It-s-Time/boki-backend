package com.boki.backend.domain.exchange.service;

import com.boki.backend.domain.exchange.dto.request.ApiKeySaveRequest;
import com.boki.backend.domain.exchange.dto.response.ApiKeySaveResponse;
import com.boki.backend.domain.exchange.dto.response.ApiKeyStatusResponse;

public interface ExchangeApiKeyService {

    ApiKeySaveResponse saveCredential(Long memberId, ApiKeySaveRequest request);

    ApiKeySaveResponse saveVerifiedCredential(Long memberId, ApiKeySaveRequest request);

    ApiKeyStatusResponse getApiKeyStatus(Long memberId);
}
