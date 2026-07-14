package com.boki.backend.domain.exchange.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.boki.backend.domain.exchange.exception.ExchangeErrorCode;
import com.boki.backend.global.apiPayload.exception.GeneralException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class UpbitRestClientTest {

    private static final String UPBIT_BASE_URL = "https://api.upbit.com";
    private static final String OUT_OF_SCOPE_BODY = """
            {"error":{"name":"out_of_scope","message":"권한이 부족합니다."}}
            """;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockRestServiceServer server;
    private ExecutorService permissionValidationExecutor;
    private UpbitRestClient upbitRestClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder().baseUrl(UPBIT_BASE_URL);
        server = MockRestServiceServer.bindTo(restClientBuilder)
                .ignoreExpectOrder(true)
                .build();
        permissionValidationExecutor = Executors.newSingleThreadExecutor();
        upbitRestClient = new UpbitRestClient(
                restClientBuilder.build(),
                new UpbitJwtProvider(),
                objectMapper,
                permissionValidationExecutor
        );
    }

    @AfterEach
    void tearDown() {
        permissionValidationExecutor.shutdownNow();
    }

    @Test
    void getClosedOrdersUsesDoneStateArrayAndUpbitSupportedLimit() {
        server.expect(request -> {
                    String requestUri = request.getURI().toString();
                    assertThat(requestUri).contains("states%5B%5D=done");
                    assertThat(requestUri).doesNotContain("state=done");
                    assertThat(requestUri).contains("limit=1000");
                })
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        upbitRestClient.getClosedOrders(
                "access-key",
                "secret-key",
                LocalDateTime.of(2026, 5, 28, 0, 0),
                LocalDateTime.of(2026, 6, 4, 0, 0)
        );

        server.verify();
    }

    @Test
    void validateCredentialPermissionsAcceptsOnlyAssetAndOrderInquiryPermissions() {
        expectValidApiKey();
        expectGetPermission("/v1/accounts", null, ProbeResponse.GRANTED);
        expectGetPermission("/v1/orders/closed", "limit=1", ProbeResponse.GRANTED);
        expectGetPermission("/v1/withdraws", "limit=1", ProbeResponse.DENIED_401);
        expectGetPermission("/v1/deposits", "limit=1", ProbeResponse.DENIED_403);
        expectDeniedOrderCreationAndVerifyQueryHash();

        upbitRestClient.validateCredentialPermissions("access-key", "secret-key");

        server.verify();
    }

    @ParameterizedTest
    @EnumSource(RequiredPermission.class)
    void validateCredentialPermissionsRejectsMissingRequiredPermission(RequiredPermission missingPermission) {
        expectValidApiKey();
        expectGetPermission(
                "/v1/accounts",
                null,
                missingPermission == RequiredPermission.ASSET_INQUIRY
                        ? ProbeResponse.DENIED_403
                        : ProbeResponse.GRANTED
        );
        expectGetPermission(
                "/v1/orders/closed",
                "limit=1",
                missingPermission == RequiredPermission.ORDER_INQUIRY
                        ? ProbeResponse.DENIED_401
                        : ProbeResponse.GRANTED
        );
        expectGetPermission("/v1/withdraws", "limit=1", ProbeResponse.DENIED_403);
        expectGetPermission("/v1/deposits", "limit=1", ProbeResponse.DENIED_403);
        expectOrderCreation(ProbeResponse.DENIED_403);

        assertExchangeError(
                () -> upbitRestClient.validateCredentialPermissions("access-key", "secret-key"),
                ExchangeErrorCode.REQUIRED_PERMISSION_MISSING
        );
        server.verify();
    }

    @ParameterizedTest
    @EnumSource(UnnecessaryPermission.class)
    void validateCredentialPermissionsRejectsDetectableUnnecessaryPermission(
            UnnecessaryPermission unnecessaryPermission
    ) {
        expectValidApiKey();
        expectGetPermission("/v1/accounts", null, ProbeResponse.GRANTED);
        expectGetPermission("/v1/orders/closed", "limit=1", ProbeResponse.GRANTED);
        expectGetPermission(
                "/v1/withdraws",
                "limit=1",
                unnecessaryPermission == UnnecessaryPermission.WITHDRAWAL_INQUIRY
                        ? ProbeResponse.GRANTED
                        : ProbeResponse.DENIED_403
        );
        expectGetPermission(
                "/v1/deposits",
                "limit=1",
                unnecessaryPermission == UnnecessaryPermission.DEPOSIT_INQUIRY
                        ? ProbeResponse.GRANTED
                        : ProbeResponse.DENIED_403
        );
        expectOrderCreation(
                unnecessaryPermission == UnnecessaryPermission.ORDER_CREATION
                        ? ProbeResponse.ORDER_BUSINESS_ERROR
                        : ProbeResponse.DENIED_403
        );

        assertExchangeError(
                () -> upbitRestClient.validateCredentialPermissions("access-key", "secret-key"),
                ExchangeErrorCode.UNNECESSARY_PERMISSION_INCLUDED
        );
        server.verify();
    }

    @Test
    void validateCredentialPermissionsRejectsInvalidApiKeyBeforePermissionProbes() {
        server.expect(requestTo(UPBIT_BASE_URL + "/v1/api_keys"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"error":{"name":"expired_access_key","message":"API Key가 만료되었습니다."}}
                                """));

        assertExchangeError(
                () -> upbitRestClient.validateCredentialPermissions("access-key", "secret-key"),
                ExchangeErrorCode.INVALID_CREDENTIALS
        );
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {429, 503})
    void validateCredentialPermissionsReturnsApiFailureForUpbitFailure(int statusCode) {
        server.expect(requestTo(UPBIT_BASE_URL + "/v1/api_keys"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.valueOf(statusCode)));

        assertExchangeError(
                () -> upbitRestClient.validateCredentialPermissions("access-key", "secret-key"),
                ExchangeErrorCode.API_REQUEST_FAILED
        );
        server.verify();
    }

    @Test
    void validateCredentialPermissionsReturnsApiFailureWhenParallelProbesTimeOut() throws Exception {
        expectValidApiKey();
        CountDownLatch blockerStarted = new CountDownLatch(1);
        CountDownLatch releaseBlocker = new CountDownLatch(1);
        permissionValidationExecutor.submit(() -> {
            blockerStarted.countDown();
            try {
                releaseBlocker.await();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        });
        assertThat(blockerStarted.await(1, TimeUnit.SECONDS)).isTrue();

        try {
            assertExchangeError(
                    () -> upbitRestClient.validateCredentialPermissions("access-key", "secret-key"),
                    ExchangeErrorCode.API_REQUEST_FAILED
            );
        } finally {
            releaseBlocker.countDown();
        }
        server.verify();
    }

    private void expectValidApiKey() {
        server.expect(requestTo(UPBIT_BASE_URL + "/v1/api_keys"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
    }

    private void expectGetPermission(String path, String query, ProbeResponse response) {
        String uri = UPBIT_BASE_URL + path + (query == null ? "" : "?" + query);
        server.expect(requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(response.responseCreator());
    }

    private void expectOrderCreation(ProbeResponse response) {
        server.expect(requestTo(UPBIT_BASE_URL + "/v1/orders/test"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(response.responseCreator());
    }

    private void expectDeniedOrderCreationAndVerifyQueryHash() {
        server.expect(requestTo(UPBIT_BASE_URL + "/v1/orders/test"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> {
                    MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
                    JsonNode body = objectMapper.readTree(mockRequest.getBodyAsString());
                    assertThat(body.path("market").asText()).isEqualTo("KRW-BTC");
                    assertThat(body.path("side").asText()).isEqualTo("bid");
                    assertThat(body.path("price").asText()).isEqualTo("5000");
                    assertThat(body.path("ord_type").asText()).isEqualTo("price");

                    String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                    assertThat(authorization).startsWith("Bearer ");
                    String token = authorization.substring("Bearer ".length());
                    String payloadJson = new String(
                            Base64.getUrlDecoder().decode(token.split("\\.")[1]),
                            StandardCharsets.UTF_8
                    );
                    JsonNode payload = objectMapper.readTree(payloadJson);
                    assertThat(payload.path("query_hash").asText()).isEqualTo(sha512(
                            "market=KRW-BTC&side=bid&price=5000&ord_type=price"
                    ));
                })
                .andRespond(ProbeResponse.DENIED_401.responseCreator());
    }

    private String sha512(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-512")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new AssertionError("SHA-512 digest is unavailable", exception);
        }
    }

    private void assertExchangeError(Runnable request, ExchangeErrorCode expectedErrorCode) {
        assertThatThrownBy(request::run)
                .isInstanceOfSatisfying(
                        GeneralException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(expectedErrorCode)
                );
    }

    private enum RequiredPermission {
        ASSET_INQUIRY,
        ORDER_INQUIRY
    }

    private enum UnnecessaryPermission {
        WITHDRAWAL_INQUIRY,
        DEPOSIT_INQUIRY,
        ORDER_CREATION
    }

    private enum ProbeResponse {
        GRANTED,
        DENIED_401,
        DENIED_403,
        ORDER_BUSINESS_ERROR;

        private org.springframework.test.web.client.ResponseCreator responseCreator() {
            return switch (this) {
                case GRANTED -> withSuccess("[]", MediaType.APPLICATION_JSON);
                case DENIED_401 -> withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(OUT_OF_SCOPE_BODY);
                case DENIED_403 -> withStatus(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(OUT_OF_SCOPE_BODY);
                case ORDER_BUSINESS_ERROR -> withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"error":{"name":"insufficient_funds_bid","message":"주문 가능 금액이 부족합니다."}}
                                """);
            };
        }
    }
}
