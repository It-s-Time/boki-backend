package com.boki.backend.domain.exchange.client;

import java.time.LocalDateTime;
import java.util.List;

public interface UpbitClient {

    void validateCredentials(String accessKey, String secretKey);

    void validateCredentialPermissions(String accessKey, String secretKey);

    List<UpbitClosedOrderResponse> getClosedOrders(
            String accessKey,
            String secretKey,
            LocalDateTime startTime,
            LocalDateTime endTime
    );
}
