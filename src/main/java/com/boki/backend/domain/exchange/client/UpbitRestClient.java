package com.boki.backend.domain.exchange.client;

import com.boki.backend.domain.exchange.exception.ExchangeErrorCode;
import com.boki.backend.global.apiPayload.exception.GeneralException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
public class UpbitRestClient implements UpbitClient {

    private static final String UPBIT_BASE_URL = "https://api.upbit.com";
    private static final String UPBIT_CLOSED_ORDER_LIMIT = "1000";
    private static final long PERMISSION_VALIDATION_TIMEOUT_SECONDS = 4;
    private static final String OUT_OF_SCOPE = "out_of_scope";
    private static final Set<String> AUTHENTICATION_ERROR_NAMES = Set.of(
            "jwt_verification",
            "expired_access_key",
            "nonce_used",
            "no_authorization_ip",
            "no_authorization_token"
    );
    private static final ZoneOffset KST_OFFSET = ZoneOffset.ofHours(9);

    private final RestClient restClient;
    private final UpbitJwtProvider upbitJwtProvider;
    private final ObjectMapper objectMapper;
    private final ExecutorService permissionValidationExecutor;

    public UpbitRestClient(
            @Qualifier("upbitApiRestClient") RestClient restClient,
            UpbitJwtProvider upbitJwtProvider,
            ObjectMapper objectMapper,
            @Qualifier("upbitPermissionValidationExecutor") ExecutorService permissionValidationExecutor
    ) {
        this.restClient = restClient;
        this.upbitJwtProvider = upbitJwtProvider;
        this.objectMapper = objectMapper;
        this.permissionValidationExecutor = permissionValidationExecutor;
    }

    @Override
    public void validateCredentials(String accessKey, String secretKey) {
        try {
            restClient
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
    public void validateCredentialPermissions(String accessKey, String secretKey) {
        validateApiKey(accessKey, secretKey);

        CompletableFuture<PermissionState> assetInquiry = probeAsync(
                () -> probeGet("/v1/accounts", Map.of(), accessKey, secretKey)
        );
        CompletableFuture<PermissionState> orderInquiry = probeAsync(
                () -> probeGet("/v1/orders/closed", Map.of("limit", "1"), accessKey, secretKey)
        );
        CompletableFuture<PermissionState> withdrawalInquiry = probeAsync(
                () -> probeGet("/v1/withdraws", Map.of("limit", "1"), accessKey, secretKey)
        );
        CompletableFuture<PermissionState> depositInquiry = probeAsync(
                () -> probeGet("/v1/deposits", Map.of("limit", "1"), accessKey, secretKey)
        );
        CompletableFuture<PermissionState> orderCreation = probeAsync(
                () -> probeOrderCreation(accessKey, secretKey)
        );

        List<CompletableFuture<PermissionState>> probes = List.of(
                assetInquiry,
                orderInquiry,
                withdrawalInquiry,
                depositInquiry,
                orderCreation
        );
        awaitPermissionProbes(probes);

        if (assetInquiry.join() != PermissionState.GRANTED
                || orderInquiry.join() != PermissionState.GRANTED) {
            throw new GeneralException(ExchangeErrorCode.REQUIRED_PERMISSION_MISSING);
        }
        if (withdrawalInquiry.join() != PermissionState.DENIED
                || depositInquiry.join() != PermissionState.DENIED
                || orderCreation.join() != PermissionState.DENIED) {
            throw new GeneralException(ExchangeErrorCode.UNNECESSARY_PERMISSION_INCLUDED);
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
            return restClient
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

    private void validateApiKey(String accessKey, String secretKey) {
        try {
            restClient
                    .get()
                    .uri("/v1/api_keys")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + upbitJwtProvider.createToken(accessKey, secretKey))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            String errorName = extractErrorName(exception);
            log.warn(
                    "Upbit API key validation failed. statusCode={}, errorName={}",
                    exception.getStatusCode(),
                    errorName
            );
            if (isUpstreamFailure(exception)) {
                throw new GeneralException(ExchangeErrorCode.API_REQUEST_FAILED);
            }
            throw new GeneralException(ExchangeErrorCode.INVALID_CREDENTIALS);
        } catch (GeneralException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("Upbit API key validation request failed", exception);
            throw new GeneralException(ExchangeErrorCode.API_REQUEST_FAILED);
        }
    }

    private PermissionState probeGet(
            String path,
            Map<String, String> queryParams,
            String accessKey,
            String secretKey
    ) {
        URI uri = createUri(path, queryParams);
        try {
            restClient
                    .get()
                    .uri(uri)
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + upbitJwtProvider.createToken(accessKey, secretKey, queryParams)
                    )
                    .retrieve()
                    .toBodilessEntity();
            return PermissionState.GRANTED;
        } catch (RestClientResponseException exception) {
            return classifyProbeFailure(path, exception, false);
        } catch (GeneralException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("Upbit permission probe request failed. endpoint={}", path, exception);
            throw new GeneralException(ExchangeErrorCode.API_REQUEST_FAILED);
        }
    }

    private PermissionState probeOrderCreation(String accessKey, String secretKey) {
        Map<String, String> requestBody = new LinkedHashMap<>();
        requestBody.put("market", "KRW-BTC");
        requestBody.put("side", "bid");
        requestBody.put("price", "5000");
        requestBody.put("ord_type", "price");

        try {
            restClient
                    .post()
                    .uri("/v1/orders/test")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + upbitJwtProvider.createToken(accessKey, secretKey, requestBody)
                    )
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity();
            return PermissionState.GRANTED;
        } catch (RestClientResponseException exception) {
            return classifyProbeFailure("/v1/orders/test", exception, true);
        } catch (GeneralException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("Upbit permission probe request failed. endpoint=/v1/orders/test", exception);
            throw new GeneralException(ExchangeErrorCode.API_REQUEST_FAILED);
        }
    }

    private PermissionState classifyProbeFailure(
            String endpoint,
            RestClientResponseException exception,
            boolean orderCreationProbe
    ) {
        String errorName = extractErrorName(exception);
        if (OUT_OF_SCOPE.equals(errorName)) {
            return PermissionState.DENIED;
        }

        log.warn(
                "Upbit permission probe failed. endpoint={}, statusCode={}, errorName={}",
                endpoint,
                exception.getStatusCode(),
                errorName
        );
        if (isUpstreamFailure(exception)) {
            throw new GeneralException(ExchangeErrorCode.API_REQUEST_FAILED);
        }
        if (AUTHENTICATION_ERROR_NAMES.contains(errorName)) {
            throw new GeneralException(ExchangeErrorCode.INVALID_CREDENTIALS);
        }
        if (orderCreationProbe && exception.getStatusCode().is4xxClientError()) {
            return PermissionState.GRANTED;
        }
        throw new GeneralException(ExchangeErrorCode.API_REQUEST_FAILED);
    }

    private CompletableFuture<PermissionState> probeAsync(Supplier<PermissionState> probe) {
        return CompletableFuture.supplyAsync(probe, permissionValidationExecutor);
    }

    private void awaitPermissionProbes(List<CompletableFuture<PermissionState>> probes) {
        try {
            CompletableFuture.allOf(probes.toArray(CompletableFuture[]::new))
                    .get(PERMISSION_VALIDATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            cancelProbes(probes);
            throw new GeneralException(ExchangeErrorCode.API_REQUEST_FAILED);
        } catch (TimeoutException exception) {
            cancelProbes(probes);
            throw new GeneralException(ExchangeErrorCode.API_REQUEST_FAILED);
        } catch (ExecutionException exception) {
            cancelProbes(probes);
            Throwable cause = exception.getCause();
            if (cause instanceof GeneralException generalException) {
                throw generalException;
            }
            throw new GeneralException(ExchangeErrorCode.API_REQUEST_FAILED);
        }
    }

    private void cancelProbes(List<CompletableFuture<PermissionState>> probes) {
        probes.forEach(probe -> probe.cancel(true));
    }

    private URI createUri(String path, Map<String, String> queryParams) {
        if (queryParams.isEmpty()) {
            return URI.create(UPBIT_BASE_URL + path);
        }
        return URI.create(UPBIT_BASE_URL + path + "?" + upbitJwtProvider.toEncodedQueryString(queryParams));
    }

    private String extractErrorName(RestClientResponseException exception) {
        try {
            JsonNode error = objectMapper.readTree(exception.getResponseBodyAsString()).path("error");
            JsonNode name = error.path("name");
            return name.isTextual() ? name.asText() : "unknown";
        } catch (Exception parsingException) {
            return "unknown";
        }
    }

    private boolean isUpstreamFailure(RestClientResponseException exception) {
        return exception.getStatusCode().value() == 429
                || exception.getStatusCode().is5xxServerError();
    }

    private String formatUpbitTime(LocalDateTime time) {
        return time.atOffset(KST_OFFSET).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private enum PermissionState {
        GRANTED,
        DENIED
    }
}
