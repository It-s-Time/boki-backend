package com.boki.backend.domain.auth.service;

import com.boki.backend.domain.auth.dto.response.AuthTokenResponse;
import com.boki.backend.domain.member.entity.SocialProvider;

public interface SocialLoginService {

    String getAuthorizationUri(SocialProvider provider, String redirectUri);

    String consumeRedirectUri(String state);

    AuthTokenResponse login(SocialProvider provider, String code);

    String createLoginCode(AuthTokenResponse response);

    AuthTokenResponse exchangeLoginCode(String loginCode);

    AuthTokenResponse reissue(String refreshToken);

    void logout(String refreshToken);
}
