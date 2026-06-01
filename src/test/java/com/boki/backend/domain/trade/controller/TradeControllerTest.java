package com.boki.backend.domain.trade.controller;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.boki.backend.domain.auth.jwt.JwtTokenProvider;
import com.boki.backend.domain.trade.repository.TradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class TradeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        tradeRepository.deleteAll();
    }

    @Test
    void createManualTradeAndReadUpdateDeleteWithFallbackUser() throws Exception {
        Long tradeId = createManualTrade();

        mockMvc.perform(get("/api/trades")
                        .header("Authorization", bearer(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess", is(true)))
                .andExpect(jsonPath("$.result", hasSize(1)))
                .andExpect(jsonPath("$.result[0].tradeId", is(tradeId.intValue())))
                .andExpect(jsonPath("$.result[0].ruleSetId", nullValue()))
                .andExpect(jsonPath("$.result[0].memberId", is(1)))
                .andExpect(jsonPath("$.result[0].inputType", is("MANUAL")))
                .andExpect(jsonPath("$.result[0].coinType", is("BTC")));

        mockMvc.perform(get("/api/trades/{id}", tradeId)
                        .header("Authorization", bearer(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.tradeId", is(tradeId.intValue())))
                .andExpect(jsonPath("$.result.price").value(closeTo(90000000.0, 0.001), Double.class))
                .andExpect(jsonPath("$.result.createdAt").exists());

        mockMvc.perform(patch("/api/trades/{id}", tradeId)
                        .header("Authorization", bearer(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleSetId": 3,
                                  "coinType": "eth",
                                  "quantity": 0.04715290,
                                  "price": 91000000
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.ruleSetId", is(3)))
                .andExpect(jsonPath("$.result.coinType", is("ETH")))
                .andExpect(jsonPath("$.result.quantity").value(closeTo(0.04715290, 0.000000001), Double.class))
                .andExpect(jsonPath("$.result.price").value(closeTo(91000000.0, 0.001), Double.class));

        mockMvc.perform(delete("/api/trades/{id}", tradeId)
                        .header("Authorization", bearer(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess", is(true)))
                .andExpect(jsonPath("$.code", is("COMMON200_1")))
                .andExpect(jsonPath("$.message", is("삭제되었습니다.")));

        mockMvc.perform(get("/api/trades/{id}", tradeId)
                        .header("Authorization", bearer(1L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("TRADE404")));
    }

    @Test
    void cannotAccessOtherUsersTrade() throws Exception {
        Long tradeId = createManualTrade();

        mockMvc.perform(get("/api/trades/{id}", tradeId)
                        .header("Authorization", bearer(2L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("TRADE403")));
    }

    @Test
    void requestWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/trades"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("COMMON401")));
    }

    @Test
    void xUserIdHeaderDoesNotAuthenticateRequest() throws Exception {
        mockMvc.perform(get("/api/trades")
                        .header("X-User-Id", "1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("COMMON401")));
    }

    @Test
    void validationErrorReturnsApiResponse() throws Exception {
        mockMvc.perform(post("/api/trades/manual")
                        .header("Authorization", bearer(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "coinType": "",
                                  "tradeType": "BUY",
                                  "price": 90000000,
                                  "quantity": -1,
                                  "tradedAt": "2026-05-08T10:30:00"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess", is(false)))
                .andExpect(jsonPath("$.code", is("COMMON400_1")));
    }

    private Long createManualTrade() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/trades/manual")
                        .header("Authorization", bearer(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleSetId": null,
                                  "coinType": "btc",
                                  "tradeType": "BUY",
                                  "price": 90000000,
                                  "quantity": 0.04715290,
                                  "tradedAt": "2026-03-23T00:00:00"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess", is(true)))
                .andExpect(jsonPath("$.code", is("COMMON201")))
                .andExpect(jsonPath("$.result.ruleSetId", nullValue()))
                .andExpect(jsonPath("$.result.memberId", is(1)))
                .andExpect(jsonPath("$.result.inputType", is("MANUAL")))
                .andExpect(jsonPath("$.result.coinType", is("BTC")))
                .andExpect(jsonPath("$.result.quantity").value(closeTo(0.04715290, 0.000000001), Double.class))
                .andReturn();

        Number id = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.result.tradeId");
        return id.longValue();
    }

    private String bearer(Long memberId) {
        return "Bearer " + jwtTokenProvider.createAccessToken(memberId);
    }
}
