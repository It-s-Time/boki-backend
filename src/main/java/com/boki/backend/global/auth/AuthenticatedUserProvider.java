package com.boki.backend.global.auth;

import com.boki.backend.global.apiPayload.code.GeneralErrorCode;
import com.boki.backend.global.apiPayload.exception.GeneralException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUserProvider {

    public Long getCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long userId && userId > 0) {
            return userId;
        }

        throw new GeneralException(GeneralErrorCode.UNAUTHORIZED);
    }
}
