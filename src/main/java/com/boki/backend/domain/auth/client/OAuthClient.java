package com.boki.backend.domain.auth.client;

import com.boki.backend.domain.member.entity.SocialProvider;

public interface OAuthClient {

    boolean supports(SocialProvider provider);

    String getAuthorizationUri(String state);

    OAuthUserInfo getUserInfo(String code);
}
