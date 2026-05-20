package com.boki.backend.domain.exchange.service;

import com.boki.backend.domain.exchange.client.UpbitClient;
import com.boki.backend.domain.exchange.dto.request.ApiKeySaveRequest;
import com.boki.backend.domain.exchange.dto.response.ApiKeySaveResponse;
import com.boki.backend.domain.exchange.entity.ApiKey;
import com.boki.backend.domain.exchange.repository.ApiKeyRepository;
import com.boki.backend.domain.exchange.util.SecretKeyEncryptor;
import com.boki.backend.global.auth.AuthenticatedUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExchangeApiKeyServiceImpl implements ExchangeApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final UpbitClient upbitClient;
    private final SecretKeyEncryptor secretKeyEncryptor;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    @Override
    @Transactional
    public ApiKeySaveResponse saveCredential(ApiKeySaveRequest request) {
        Long memberId = authenticatedUserProvider.getCurrentUserId();

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

        return new ApiKeySaveResponse(apiKey.getMemberId(), true);
    }
}
