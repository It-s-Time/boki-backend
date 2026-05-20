package com.boki.backend.global.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class AuthenticatedUserProvider {

    private static final long LOCAL_TEST_USER_ID = 1L;
    private static final String USER_ID_HEADER = "X-User-Id";

    public Long getCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long userId && userId > 0) {
            return userId;
        }

        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return LOCAL_TEST_USER_ID;
        }

        HttpServletRequest request = attributes.getRequest();
        String headerValue = request.getHeader(USER_ID_HEADER);
        if (headerValue == null || headerValue.isBlank()) {
            return LOCAL_TEST_USER_ID;
        }

        try {
            long userId = Long.parseLong(headerValue);
            return userId > 0 ? userId : LOCAL_TEST_USER_ID;
        } catch (NumberFormatException exception) {
            return LOCAL_TEST_USER_ID;
        }
    }
}
