package com.boki.backend.domain.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "boki.oauth")
public record OAuthProperties(
        Provider google,
        Provider kakao
) {

    public record Provider(
            String clientId,
            String clientSecret,
            String redirectUri
    ) {
    }
}
