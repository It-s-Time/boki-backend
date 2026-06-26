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
public class GoogleOAuthClient implements OAuthClient {

    private static final String AUTHORIZATION_URI = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URI = "https://oauth2.googleapis.com/token";
    private static final String USER_INFO_URI = "https://www.googleapis.com/oauth2/v3/userinfo";
    private static final String SCOPE = "openid email";

    private final OAuthProperties oauthProperties;
    private final RestClient restClient = RestClient.create();

    @Override
    public boolean supports(SocialProvider provider) {
        return provider == SocialProvider.GOOGLE;
    }

    @Override
    public String getAuthorizationUri(String state) {
        OAuthProperties.Provider google = getProperties();
        return UriComponentsBuilder.fromUriString(AUTHORIZATION_URI)
                .queryParam("response_type", "code")
                .queryParam("client_id", google.clientId())
                .queryParam("redirect_uri", google.redirectUri())
                .queryParam("scope", SCOPE)
                .queryParam("state", state)
                .encode()
                .build()
                .toUriString();
    }

    @Override
    public OAuthUserInfo getUserInfo(String code) {
        GoogleTokenResponse tokenResponse = requestToken(code);
        GoogleUserInfoResponse userInfo = requestUserInfo(tokenResponse.accessToken());
        if (userInfo.sub() == null || userInfo.sub().isBlank()) {
            throw new GeneralException(AuthErrorCode.OAUTH_USER_INFO_FAILED);
        }
        return new OAuthUserInfo(SocialProvider.GOOGLE, userInfo.sub(), userInfo.email());
    }

    private GoogleTokenResponse requestToken(String code) {
        OAuthProperties.Provider google = getProperties();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", google.clientId());
        form.add("client_secret", google.clientSecret());
        form.add("redirect_uri", google.redirectUri());
        form.add("code", code);

        try {
            return Objects.requireNonNull(restClient.post()
                    .uri(TOKEN_URI)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(GoogleTokenResponse.class));
        } catch (RestClientException | NullPointerException exception) {
            throw new GeneralException(AuthErrorCode.INVALID_OAUTH_CODE);
        }
    }

    private GoogleUserInfoResponse requestUserInfo(String accessToken) {
        try {
            return Objects.requireNonNull(restClient.get()
                    .uri(USER_INFO_URI)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .body(GoogleUserInfoResponse.class));
        } catch (RestClientException | NullPointerException exception) {
            throw new GeneralException(AuthErrorCode.OAUTH_USER_INFO_FAILED);
        }
    }

    private OAuthProperties.Provider getProperties() {
        OAuthProperties.Provider google = oauthProperties.google();
        if (google == null || isBlank(google.clientId()) || isBlank(google.clientSecret()) || isBlank(google.redirectUri())) {
            throw new GeneralException(AuthErrorCode.OAUTH_CONFIGURATION_MISSING);
        }
        return google;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record GoogleTokenResponse(
            @JsonProperty("access_token") String accessToken
    ) {
    }

    private record GoogleUserInfoResponse(
            String sub,
            String email
    ) {
    }
}
