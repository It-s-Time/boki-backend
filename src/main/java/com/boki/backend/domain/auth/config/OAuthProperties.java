package com.boki.backend.domain.auth.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "boki.oauth")
public record OAuthProperties(
        Provider google,
        Provider kakao,
        List<String> allowedRedirectUris
) {

    public record Provider(
            String clientId,
            String clientSecret,
            String redirectUri
    ) {
    }
}
