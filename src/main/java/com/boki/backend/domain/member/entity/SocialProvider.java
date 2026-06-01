package com.boki.backend.domain.member.entity;

import com.boki.backend.domain.auth.exception.AuthErrorCode;
import com.boki.backend.global.apiPayload.exception.GeneralException;
import java.util.Arrays;

public enum SocialProvider {
    GOOGLE,
    KAKAO;

    public static SocialProvider from(String value) {
        return Arrays.stream(values())
                .filter(provider -> provider.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new GeneralException(AuthErrorCode.UNSUPPORTED_PROVIDER));
    }
}
