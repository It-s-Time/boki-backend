package com.boki.backend.domain.auth.dto.response;

import com.boki.backend.domain.member.entity.SocialProvider;

public record AuthTokenResponse(
        Long memberId,
        String email,
        SocialProvider provider,
        String accessToken,
        String refreshToken
) {
}
