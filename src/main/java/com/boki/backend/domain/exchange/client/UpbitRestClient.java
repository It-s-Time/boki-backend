package com.boki.backend.domain.exchange.client;

import com.boki.backend.domain.exchange.exception.ExchangeErrorCode;
import com.boki.backend.global.apiPayload.exception.GeneralException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class UpbitRestClient implements UpbitClient {

    private static final String UPBIT_BASE_URL = "https://api.upbit.com";

    private final RestClient.Builder restClientBuilder;
    private final UpbitJwtProvider upbitJwtProvider;

    @Override
    public void validateCredentials(String accessKey, String secretKey) {
        try {
            restClientBuilder.baseUrl(UPBIT_BASE_URL)
                    .build()
                    .get()
                    .uri("/v1/accounts")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + upbitJwtProvider.createToken(accessKey, secretKey))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception exception) {
            throw new GeneralException(ExchangeErrorCode.INVALID_CREDENTIALS);
        }
    }

    @Override
    public List<UpbitClosedOrderResponse> getClosedOrders(String accessKey, String secretKey) {
        Map<String, String> queryParams = Map.of(
                "state", "done",
                "limit", "100",
                "order_by", "desc"
        );
        String queryString = upbitJwtProvider.toQueryString(queryParams);

        try {
            return restClientBuilder.baseUrl(UPBIT_BASE_URL)
                    .build()
                    .get()
                    .uri("/v1/orders/closed?" + queryString)
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + upbitJwtProvider.createToken(accessKey, secretKey, queryParams)
                    )
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (Exception exception) {
            throw new GeneralException(ExchangeErrorCode.API_REQUEST_FAILED);
        }
    }
}
