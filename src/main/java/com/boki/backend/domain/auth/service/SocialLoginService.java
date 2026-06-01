package com.boki.backend.domain.auth.service;

import com.boki.backend.domain.auth.dto.response.AuthTokenResponse;
import com.boki.backend.domain.member.entity.SocialProvider;

public interface SocialLoginService {

    String getAuthorizationUri(SocialProvider provider);

    AuthTokenResponse login(SocialProvider provider, String code);

    AuthTokenResponse reissue(String refreshToken);

    void logout(String refreshToken);
}
