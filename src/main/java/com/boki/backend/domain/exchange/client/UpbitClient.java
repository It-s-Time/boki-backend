package com.boki.backend.domain.exchange.client;

import java.util.List;

public interface UpbitClient {

    void validateCredentials(String accessKey, String secretKey);

    List<UpbitClosedOrderResponse> getClosedOrders(String accessKey, String secretKey);
}
