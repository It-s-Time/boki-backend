package com.boki.backend.domain.exchange.service;

import com.boki.backend.domain.exchange.client.UpbitClient;
import com.boki.backend.domain.exchange.config.UpbitTradeSyncProperties;
import com.boki.backend.domain.exchange.dto.request.ApiKeySaveRequest;
import com.boki.backend.domain.exchange.dto.response.ApiKeySaveResponse;
import com.boki.backend.domain.exchange.dto.response.ApiKeyStatusResponse;
import com.boki.backend.domain.exchange.entity.ApiKey;
import com.boki.backend.domain.exchange.repository.ApiKeyRepository;
import com.boki.backend.domain.exchange.util.SecretKeyEncryptor;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExchangeApiKeyServiceImpl implements ExchangeApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final UpbitClient upbitClient;
    private final SecretKeyEncryptor secretKeyEncryptor;
    private final ExchangeTradeSyncAsyncExecutor exchangeTradeSyncAsyncExecutor;
    private final Clock clock;
    private final UpbitTradeSyncProperties syncProperties;

    @Override
    @Transactional
    public ApiKeySaveResponse saveCredential(Long memberId, ApiKeySaveRequest request) {
        upbitClient.validateCredentials(request.accessKey(), request.secretKey());
        String encryptedSecretKey = secretKeyEncryptor.encrypt(request.secretKey());

        ApiKey apiKey = apiKeyRepository.findByMemberId(memberId)
                .map(existingCredential -> {
                    existingCredential.update(request.accessKey(), encryptedSecretKey);
                    return existingCredential;
                })
                .orElseGet(() -> apiKeyRepository.save(ApiKey.builder()
                        .memberId(memberId)
                        .accessKey(request.accessKey())
                        .secretKey(encryptedSecretKey)
                        .build()));
        apiKey.markTradeSyncDue(LocalDateTime.now(clock));
        startInitialTradeSyncAfterCommit(apiKey.getMemberApiKeyId());

        return new ApiKeySaveResponse(apiKey.getMemberId(), true);
    }

    @Override
    public ApiKeyStatusResponse getApiKeyStatus(Long memberId) {
        return new ApiKeyStatusResponse(apiKeyRepository.existsByMemberId(memberId));
    }

    private void startInitialTradeSyncAfterCommit(Long memberApiKeyId) {
        if (!syncProperties.enabled()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            exchangeTradeSyncAsyncExecutor.syncApiKeyTrades(memberApiKeyId);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                exchangeTradeSyncAsyncExecutor.syncApiKeyTrades(memberApiKeyId);
            }
        });
    }
}
