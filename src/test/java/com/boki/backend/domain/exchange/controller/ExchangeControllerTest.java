package com.boki.backend.domain.exchange.controller;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.boki.backend.domain.trade.repository.TradeRepository;
import com.boki.backend.domain.exchange.client.UpbitClient;
import com.boki.backend.domain.exchange.client.UpbitClosedOrderResponse;
import com.boki.backend.domain.exchange.entity.ApiKey;
import com.boki.backend.domain.exchange.exception.ExchangeErrorCode;
import com.boki.backend.domain.exchange.repository.ApiKeyRepository;
import com.boki.backend.domain.exchange.util.SecretKeyEncryptor;
import com.boki.backend.global.apiPayload.exception.GeneralException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class ExchangeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private SecretKeyEncryptor secretKeyEncryptor;

    @MockitoBean
    private UpbitClient upbitClient;

    @BeforeEach
    void setUp() {
        tradeRepository.deleteAll();
        apiKeyRepository.deleteAll();
    }

    @Test
    void saveCredentialCreatesEncryptedCredential() throws Exception {
        mockMvc.perform(post("/api/exchange/api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accessKey": "access-key",
                                  "secretKey": "secret-key"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess", is(true)))
                .andExpect(jsonPath("$.result.memberId", is(1)))
                .andExpect(jsonPath("$.result.connected", is(true)));

        ApiKey credential = apiKeyRepository.findByMemberId(1L).orElseThrow();
        org.hamcrest.MatcherAssert.assertThat(credential.getAccessKey(), is("access-key"));
        org.hamcrest.MatcherAssert.assertThat(credential.getSecretKey(), not("secret-key"));
        org.hamcrest.MatcherAssert.assertThat(secretKeyEncryptor.decrypt(credential.getSecretKey()), is("secret-key"));
        verify(upbitClient).validateCredentials("access-key", "secret-key");
    }

    @Test
    void saveCredentialUpdatesExistingCredential() throws Exception {
        apiKeyRepository.save(ApiKey.builder()
                .memberId(1L)
                .accessKey("old-access-key")
                .secretKey(secretKeyEncryptor.encrypt("old-secret-key"))
                .build());

        mockMvc.perform(post("/api/exchange/api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accessKey": "new-access-key",
                                  "secretKey": "new-secret-key"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.connected", is(true)));

        List<ApiKey> credentials = apiKeyRepository.findAll();
        org.hamcrest.MatcherAssert.assertThat(credentials, hasSize(1));
        org.hamcrest.MatcherAssert.assertThat(credentials.get(0).getAccessKey(), is("new-access-key"));
        org.hamcrest.MatcherAssert.assertThat(
                secretKeyEncryptor.decrypt(credentials.get(0).getSecretKey()),
                is("new-secret-key")
        );
    }

    @Test
    void saveCredentialFailsWhenUpbitRejectsKeys() throws Exception {
        doThrow(new GeneralException(ExchangeErrorCode.INVALID_CREDENTIALS))
                .when(upbitClient)
                .validateCredentials(anyString(), anyString());

        mockMvc.perform(post("/api/exchange/api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accessKey": "bad-access-key",
                                  "secretKey": "bad-secret-key"
                                }
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("EXCHANGE400")));
    }

    @Test
    void syncUpbitTradesFailsWithoutCredential() throws Exception {
        mockMvc.perform(post("/api/exchange/sync/trades"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("EXCHANGE404")));
    }

    @Test
    void syncUpbitTradesCreatesApiTradesAndSkipsDuplicates() throws Exception {
        apiKeyRepository.save(ApiKey.builder()
                .memberId(1L)
                .accessKey("access-key")
                .secretKey(secretKeyEncryptor.encrypt("secret-key"))
                .build());

        when(upbitClient.getClosedOrders("access-key", "secret-key"))
                .thenReturn(List.of(new UpbitClosedOrderResponse(
                        "upbit-order-uuid",
                        "bid",
                        "KRW-BTC",
                        new BigDecimal("106038000"),
                        new BigDecimal("0.04715290"),
                        new BigDecimal("0.04715290"),
                        new BigDecimal("4999999.98220000"),
                        OffsetDateTime.parse("2026-03-23T14:35:10+09:00")
                )));

        mockMvc.perform(post("/api/exchange/sync/trades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.syncedCount", is(1)))
                .andExpect(jsonPath("$.result.skippedCount", is(0)))
                .andExpect(jsonPath("$.result.trades[0].inputType", is("API")))
                .andExpect(jsonPath("$.result.trades[0].coinType", is("KRW-BTC")))
                .andExpect(jsonPath("$.result.trades[0].tradeType", is("BUY")))
                .andExpect(jsonPath("$.result.trades[0].externalSource").doesNotExist())
                .andExpect(jsonPath("$.result.trades[0].externalTradeId", is("upbit-order-uuid")))
                .andExpect(jsonPath("$.result.trades[0].quantity").value(closeTo(0.04715290, 0.000000001), Double.class));

        mockMvc.perform(post("/api/exchange/sync/trades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.syncedCount", is(0)))
                .andExpect(jsonPath("$.result.skippedCount", is(1)));

        org.hamcrest.MatcherAssert.assertThat(tradeRepository.findAll(), hasSize(1));
    }
}
