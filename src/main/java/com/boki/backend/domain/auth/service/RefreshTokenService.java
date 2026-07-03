package com.boki.backend.domain.auth.service;

import com.boki.backend.domain.auth.exception.AuthErrorCode;
import com.boki.backend.global.apiPayload.exception.GeneralException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String REFRESH_TOKEN_KEY_PREFIX = "RT:";

    private final StringRedisTemplate stringRedisTemplate;

    public void save(Long memberId, String refreshToken, long expirationMs) {
        stringRedisTemplate.opsForValue()
                .set(getKey(memberId), refreshToken, Duration.ofMillis(expirationMs));
    }

    public void validate(Long memberId, String refreshToken) {
        String storedRefreshToken = stringRedisTemplate.opsForValue().get(getKey(memberId));
        if (storedRefreshToken == null) {
            throw new GeneralException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }
        if (!storedRefreshToken.equals(refreshToken)) {
            throw new GeneralException(AuthErrorCode.REFRESH_TOKEN_MISMATCH);
        }
    }

    public void delete(Long memberId) {
        stringRedisTemplate.delete(getKey(memberId));
    }

    private String getKey(Long memberId) {
        return REFRESH_TOKEN_KEY_PREFIX + memberId;
    }
}
