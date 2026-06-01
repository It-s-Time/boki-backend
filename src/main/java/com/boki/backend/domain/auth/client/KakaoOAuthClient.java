package com.boki.backend.domain.auth.client;

import com.boki.backend.domain.auth.config.OAuthProperties;
import com.boki.backend.domain.auth.exception.AuthErrorCode;
import com.boki.backend.domain.member.entity.SocialProvider;
import com.boki.backend.global.apiPayload.exception.GeneralException;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class KakaoOAuthClient implements OAuthClient {

    private static final String AUTHORIZATION_URI = "https://kauth.kakao.com/oauth/authorize";
    private static final String TOKEN_URI = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URI = "https://kapi.kakao.com/v2/user/me";

    private final OAuthProperties oauthProperties;
    private final RestClient restClient = RestClient.create();

    @Override
    public boolean supports(SocialProvider provider) {
        return provider == SocialProvider.KAKAO;
    }

    @Override
    public String getAuthorizationUri() {
        OAuthProperties.Provider kakao = getProperties();
        return UriComponentsBuilder.fromUriString(AUTHORIZATION_URI)
                .queryParam("response_type", "code")
                .queryParam("client_id", kakao.clientId())
                .queryParam("redirect_uri", kakao.redirectUri())
                .build()
                .toUriString();
    }

    @Override
    public OAuthUserInfo getUserInfo(String code) {
        KakaoTokenResponse tokenResponse = requestToken(code);
        KakaoUserInfoResponse userInfo = requestUserInfo(tokenResponse.accessToken());
        if (userInfo.id() == null) {
            throw new GeneralException(AuthErrorCode.OAUTH_USER_INFO_FAILED);
        }
        String email = userInfo.kakaoAccount() == null ? null : userInfo.kakaoAccount().email();
        return new OAuthUserInfo(SocialProvider.KAKAO, String.valueOf(userInfo.id()), email);
    }

    private KakaoTokenResponse requestToken(String code) {
        OAuthProperties.Provider kakao = getProperties();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", kakao.clientId());
        form.add("redirect_uri", kakao.redirectUri());
        form.add("code", code);
        if (!isBlank(kakao.clientSecret())) {
            form.add("client_secret", kakao.clientSecret());
        }

        try {
            return Objects.requireNonNull(restClient.post()
                    .uri(TOKEN_URI)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(KakaoTokenResponse.class));
        } catch (RestClientException | NullPointerException exception) {
            throw new GeneralException(AuthErrorCode.INVALID_OAUTH_CODE);
        }
    }

    private KakaoUserInfoResponse requestUserInfo(String accessToken) {
        try {
            return Objects.requireNonNull(restClient.get()
                    .uri(USER_INFO_URI)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .body(KakaoUserInfoResponse.class));
        } catch (RestClientException | NullPointerException exception) {
            throw new GeneralException(AuthErrorCode.OAUTH_USER_INFO_FAILED);
        }
    }

    private OAuthProperties.Provider getProperties() {
        OAuthProperties.Provider kakao = oauthProperties.kakao();
        if (kakao == null || isBlank(kakao.clientId()) || isBlank(kakao.redirectUri())) {
            throw new GeneralException(AuthErrorCode.OAUTH_CONFIGURATION_MISSING);
        }
        return kakao;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record KakaoTokenResponse(
            @JsonProperty("access_token") String accessToken
    ) {
    }

    private record KakaoUserInfoResponse(
            Long id,
            @JsonProperty("kakao_account") KakaoAccount kakaoAccount
    ) {
    }

    private record KakaoAccount(
            String email
    ) {
    }
}
