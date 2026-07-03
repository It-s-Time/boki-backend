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
                .andExpect(jsonPath("$.result.totalAmount").value(closeTo(4500000.0, 0.001), Double.class))
                .andExpect(jsonPath("$.result.quantity").value(closeTo(0.05, 0.000000001), Double.class))
                .andExpect(jsonPath("$.result.createdAt").exists());

        mockMvc.perform(patch("/api/trades/{id}", tradeId)
                        .header("Authorization", bearer(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleSetId": 3,
                                  "coinType": "eth",
                                  "price": 91000000,
                                  "totalAmount": 4550000
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.ruleSetId", is(3)))
                .andExpect(jsonPath("$.result.coinType", is("ETH")))
                .andExpect(jsonPath("$.result.price").value(closeTo(91000000.0, 0.001), Double.class))
                .andExpect(jsonPath("$.result.totalAmount").value(closeTo(4550000.0, 0.001), Double.class))
                .andExpect(jsonPath("$.result.quantity").value(closeTo(0.05, 0.000000001), Double.class));

        mockMvc.perform(patch("/api/trades/{id}", tradeId)
                        .header("Authorization", bearer(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "price": 70000000
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.price").value(closeTo(70000000.0, 0.001), Double.class))
                .andExpect(jsonPath("$.result.totalAmount").value(closeTo(4550000.0, 0.001), Double.class))
                .andExpect(jsonPath("$.result.quantity").value(closeTo(0.065, 0.000000001), Double.class));

        mockMvc.perform(patch("/api/trades/{id}", tradeId)
                        .header("Authorization", bearer(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "totalAmount": 3500000
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.price").value(closeTo(70000000.0, 0.001), Double.class))
                .andExpect(jsonPath("$.result.totalAmount").value(closeTo(3500000.0, 0.001), Double.class))
                .andExpect(jsonPath("$.result.quantity").value(closeTo(0.05, 0.000000001), Double.class));

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
    void getTradeCalendarReturnsOnlyOwnedTradeDaysWithCountsInAscendingDateOrder() throws Exception {
        createManualTrade(1L, "BUY", "2026-07-03T09:00:00");
        createManualTrade(1L, "SELL", "2026-07-03T15:00:00");
        createManualTrade(1L, "BUY", "2026-07-10T11:00:00");
        createManualTrade(1L, "BUY", "2026-08-01T00:00:00");
        createManualTrade(2L, "SELL", "2026-07-20T13:00:00");

        mockMvc.perform(get("/api/trades/calendar")
                        .param("year", "2026")
                        .param("month", "7")
                        .header("Authorization", bearer(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess", is(true)))
                .andExpect(jsonPath("$.result.year", is(2026)))
                .andExpect(jsonPath("$.result.month", is(7)))
                .andExpect(jsonPath("$.result.days", hasSize(2)))
                .andExpect(jsonPath("$.result.days[0].date", is("2026-07-03")))
                .andExpect(jsonPath("$.result.days[0].hasTrade", is(true)))
                .andExpect(jsonPath("$.result.days[0].tradeCount", is(2)))
                .andExpect(jsonPath("$.result.days[0].buyCount", is(1)))
                .andExpect(jsonPath("$.result.days[0].sellCount", is(1)))
                .andExpect(jsonPath("$.result.days[1].date", is("2026-07-10")))
                .andExpect(jsonPath("$.result.days[1].hasTrade", is(true)))
                .andExpect(jsonPath("$.result.days[1].tradeCount", is(1)))
                .andExpect(jsonPath("$.result.days[1].buyCount", is(1)))
                .andExpect(jsonPath("$.result.days[1].sellCount", is(0)));
    }

    @Test
    void getTradesWithDateReturnsOnlyOwnedTradesForThatDateInExistingOrder() throws Exception {
        createManualTrade(1L, "BUY", "2026-07-02T23:59:59");
        Long morningTradeId = createManualTrade(1L, "BUY", "2026-07-03T09:00:00");
        Long afternoonTradeId = createManualTrade(1L, "SELL", "2026-07-03T15:00:00");
        createManualTrade(1L, "BUY", "2026-07-04T00:00:00");
        createManualTrade(2L, "SELL", "2026-07-03T18:00:00");

        mockMvc.perform(get("/api/trades")
                        .param("date", "2026-07-03")
                        .header("Authorization", bearer(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess", is(true)))
                .andExpect(jsonPath("$.result", hasSize(2)))
                .andExpect(jsonPath("$.result[0].tradeId", is(afternoonTradeId.intValue())))
                .andExpect(jsonPath("$.result[0].tradedAt", is("2026-07-03T15:00:00")))
                .andExpect(jsonPath("$.result[0].tradeType", is("SELL")))
                .andExpect(jsonPath("$.result[1].tradeId", is(morningTradeId.intValue())))
                .andExpect(jsonPath("$.result[1].tradedAt", is("2026-07-03T09:00:00")))
                .andExpect(jsonPath("$.result[1].tradeType", is("BUY")));
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
                                  "totalAmount": -1,
                                  "tradedAt": "2026-05-08T10:30:00"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess", is(false)))
                .andExpect(jsonPath("$.code", is("COMMON400_1")));
    }

    private Long createManualTrade() throws Exception {
        return createManualTrade(1L, "BUY", "2026-03-23T00:00:00");
    }

    private Long createManualTrade(Long memberId, String tradeType, String tradedAt) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/trades/manual")
                        .header("Authorization", bearer(memberId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "ruleSetId": null,
                                  "coinType": "btc",
                                  "tradeType": "%s",
                                  "price": 90000000,
                                  "totalAmount": 4500000,
                                  "tradedAt": "%s"
                                }
                                """, tradeType, tradedAt)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess", is(true)))
                .andExpect(jsonPath("$.code", is("COMMON201")))
                .andExpect(jsonPath("$.result.ruleSetId", nullValue()))
                .andExpect(jsonPath("$.result.memberId", is(memberId.intValue())))
                .andExpect(jsonPath("$.result.inputType", is("MANUAL")))
                .andExpect(jsonPath("$.result.coinType", is("BTC")))
                .andExpect(jsonPath("$.result.totalAmount").value(closeTo(4500000.0, 0.001), Double.class))
                .andExpect(jsonPath("$.result.quantity").value(closeTo(0.05, 0.000000001), Double.class))
                .andReturn();

        Number id = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.result.tradeId");
        return id.longValue();
    }

    private String bearer(Long memberId) {
        return "Bearer " + jwtTokenProvider.createAccessToken(memberId);
    }
}
