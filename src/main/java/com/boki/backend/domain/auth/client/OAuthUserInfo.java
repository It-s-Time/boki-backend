package com.boki.backend.domain.auth.client;

import com.boki.backend.domain.member.entity.SocialProvider;

public record OAuthUserInfo(
        SocialProvider provider,
        String providerId,
        String email
) {
}
