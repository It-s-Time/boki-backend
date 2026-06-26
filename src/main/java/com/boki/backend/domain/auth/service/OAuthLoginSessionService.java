package com.boki.backend.domain.auth.service;

import com.boki.backend.domain.auth.config.OAuthProperties;
import com.boki.backend.domain.auth.dto.response.AuthTokenResponse;
import com.boki.backend.domain.auth.exception.AuthErrorCode;
import com.boki.backend.global.apiPayload.exception.GeneralException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OAuthLoginSessionService {

    private static final String OAUTH_STATE_KEY_PREFIX = "OAUTH_STATE:";
    private static final String LOGIN_CODE_KEY_PREFIX = "LOGIN_CODE:";
    private static final Duration OAUTH_STATE_TTL = Duration.ofMinutes(5);
    private static final Duration LOGIN_CODE_TTL = Duration.ofMinutes(3);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final OAuthProperties oauthProperties;

    public String createState(String redirectUri) {
        validateRedirectUri(redirectUri);
        String state = UUID.randomUUID().toString();
        stringRedisTemplate.opsForValue()
                .set(OAUTH_STATE_KEY_PREFIX + state, redirectUri, OAUTH_STATE_TTL);
        return state;
    }

    public String consumeState(String state) {
        String redirectUri = stringRedisTemplate.opsForValue()
                .getAndDelete(OAUTH_STATE_KEY_PREFIX + state);
        if (redirectUri == null) {
            throw new GeneralException(AuthErrorCode.INVALID_OAUTH_STATE);
        }
        return redirectUri;
    }

    public String createLoginCode(AuthTokenResponse response) {
        String loginCode = UUID.randomUUID().toString();
        try {
            String serializedResponse = objectMapper.writeValueAsString(response);
            stringRedisTemplate.opsForValue()
                    .set(LOGIN_CODE_KEY_PREFIX + loginCode, serializedResponse, LOGIN_CODE_TTL);
            return loginCode;
        } catch (JsonProcessingException exception) {
            throw new GeneralException(AuthErrorCode.LOGIN_CODE_SERIALIZATION_FAILED);
        }
    }

    public AuthTokenResponse consumeLoginCode(String loginCode) {
        String serializedResponse = stringRedisTemplate.opsForValue()
                .getAndDelete(LOGIN_CODE_KEY_PREFIX + loginCode);
        if (serializedResponse == null) {
            throw new GeneralException(AuthErrorCode.LOGIN_CODE_NOT_FOUND);
        }
        try {
            return objectMapper.readValue(serializedResponse, AuthTokenResponse.class);
        } catch (JsonProcessingException exception) {
            throw new GeneralException(AuthErrorCode.LOGIN_CODE_SERIALIZATION_FAILED);
        }
    }

    private void validateRedirectUri(String redirectUri) {
        List<String> allowedRedirectUris = oauthProperties.allowedRedirectUris();
        if (allowedRedirectUris == null || !allowedRedirectUris.contains(redirectUri)) {
            throw new GeneralException(AuthErrorCode.OAUTH_REDIRECT_URI_NOT_ALLOWED);
        }
    }
}
