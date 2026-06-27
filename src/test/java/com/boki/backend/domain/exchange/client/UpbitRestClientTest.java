package com.boki.backend.domain.exchange.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.test.web.client.MockRestServiceServer;

class UpbitRestClientTest {

    @Test
    void getClosedOrdersUsesDoneStateArrayAndUpbitSupportedLimit() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        UpbitRestClient upbitRestClient = new UpbitRestClient(restClientBuilder, new UpbitJwtProvider());
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
}
