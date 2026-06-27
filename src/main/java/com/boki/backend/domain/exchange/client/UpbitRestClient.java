package com.boki.backend.domain.exchange.client;

import com.boki.backend.domain.exchange.exception.ExchangeErrorCode;
import com.boki.backend.global.apiPayload.exception.GeneralException;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpbitRestClient implements UpbitClient {

    private static final String UPBIT_BASE_URL = "https://api.upbit.com";
    private static final String UPBIT_CLOSED_ORDER_LIMIT = "1000";
    private static final ZoneOffset KST_OFFSET = ZoneOffset.ofHours(9);

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
    public List<UpbitClosedOrderResponse> getClosedOrders(
            String accessKey,
            String secretKey,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("states[]", "done");
        queryParams.put("limit", UPBIT_CLOSED_ORDER_LIMIT);
        queryParams.put("order_by", "asc");
        queryParams.put("start_time", formatUpbitTime(startTime));
        queryParams.put("end_time", formatUpbitTime(endTime));
        String queryString = upbitJwtProvider.toEncodedQueryString(queryParams);
        URI uri = URI.create(UPBIT_BASE_URL + "/v1/orders/closed?" + queryString);

        try {
            return restClientBuilder.build()
                    .get()
                    .uri(uri)
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + upbitJwtProvider.createToken(accessKey, secretKey, queryParams)
                    )
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (RestClientResponseException exception) {
            log.warn(
                    "Upbit closed orders request failed. statusCode={}, responseBody={}, startTime={}, endTime={}",
                    exception.getStatusCode(),
                    exception.getResponseBodyAsString(),
                    startTime,
                    endTime,
                    exception
            );
            throw new GeneralException(ExchangeErrorCode.API_REQUEST_FAILED);
        } catch (Exception exception) {
            log.warn(
                    "Upbit closed orders request failed. startTime={}, endTime={}",
                    startTime,
                    endTime,
                    exception
            );
            throw new GeneralException(ExchangeErrorCode.API_REQUEST_FAILED);
        }
    }

    private String formatUpbitTime(LocalDateTime time) {
        return time.atOffset(KST_OFFSET).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
