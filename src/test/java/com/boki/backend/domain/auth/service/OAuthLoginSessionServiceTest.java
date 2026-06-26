package com.boki.backend.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.boki.backend.domain.auth.config.OAuthProperties;
import com.boki.backend.domain.auth.dto.response.AuthTokenResponse;
import com.boki.backend.domain.auth.exception.AuthErrorCode;
import com.boki.backend.domain.member.entity.SocialProvider;
import com.boki.backend.global.apiPayload.exception.GeneralException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class OAuthLoginSessionServiceTest {

    private OAuthLoginSessionService oauthLoginSessionService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        OAuthProperties oauthProperties = new OAuthProperties(
                null,
                null,
                List.of("boki://auth/callback")
        );
        oauthLoginSessionService = new OAuthLoginSessionService(
                stringRedisTemplate,
                new ObjectMapper(),
                oauthProperties
        );
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void createStateStoresAllowedRedirectUri() {
        String state = oauthLoginSessionService.createState("boki://auth/callback");

        assertThat(state).isNotBlank();
        verify(valueOperations).set(
                eq("OAUTH_STATE:" + state),
                eq("boki://auth/callback"),
                eq(Duration.ofMinutes(5))
        );
    }

    @Test
    void createStateRejectsNotAllowedRedirectUri() {
        assertThatThrownBy(() -> oauthLoginSessionService.createState("evil://auth/callback"))
                .isInstanceOf(GeneralException.class)
                .extracting("code")
                .isEqualTo(AuthErrorCode.OAUTH_REDIRECT_URI_NOT_ALLOWED);
    }

    @Test
    void consumeStateReturnsRedirectUriAndDeletesState() {
        when(valueOperations.getAndDelete("OAUTH_STATE:state")).thenReturn("boki://auth/callback");

        String redirectUri = oauthLoginSessionService.consumeState("state");

        assertThat(redirectUri).isEqualTo("boki://auth/callback");
    }

    @Test
    void consumeStateFailsWhenStateExpired() {
        when(valueOperations.getAndDelete("OAUTH_STATE:expired")).thenReturn(null);

        assertThatThrownBy(() -> oauthLoginSessionService.consumeState("expired"))
                .isInstanceOf(GeneralException.class)
                .extracting("code")
                .isEqualTo(AuthErrorCode.INVALID_OAUTH_STATE);
    }

    @Test
    void createLoginCodeStoresAuthTokenResponse() {
        AuthTokenResponse response = new AuthTokenResponse(
                1L,
                "test@test.com",
                SocialProvider.KAKAO,
                "access-token",
                "refresh-token"
        );

        String loginCode = oauthLoginSessionService.createLoginCode(response);

        assertThat(loginCode).isNotBlank();
        verify(valueOperations).set(
                eq("LOGIN_CODE:" + loginCode),
                any(String.class),
                eq(Duration.ofMinutes(3))
        );
    }

    @Test
    void consumeLoginCodeReturnsTokenAndDeletesCode() throws Exception {
        AuthTokenResponse response = new AuthTokenResponse(
                1L,
                "test@test.com",
                SocialProvider.KAKAO,
                "access-token",
                "refresh-token"
        );
        String serializedResponse = new ObjectMapper().writeValueAsString(response);
        when(valueOperations.getAndDelete("LOGIN_CODE:login-code")).thenReturn(serializedResponse);

        AuthTokenResponse result = oauthLoginSessionService.consumeLoginCode("login-code");

        assertThat(result).isEqualTo(response);
    }

    @Test
    void consumeLoginCodeFailsWhenCodeExpiredOrReused() {
        when(valueOperations.getAndDelete("LOGIN_CODE:used-code")).thenReturn(null);

        assertThatThrownBy(() -> oauthLoginSessionService.consumeLoginCode("used-code"))
                .isInstanceOf(GeneralException.class)
                .extracting("code")
                .isEqualTo(AuthErrorCode.LOGIN_CODE_NOT_FOUND);
    }
}
