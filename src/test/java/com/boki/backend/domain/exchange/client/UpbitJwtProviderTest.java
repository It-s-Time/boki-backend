package com.boki.backend.domain.exchange.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UpbitJwtProviderTest {

    private final UpbitJwtProvider upbitJwtProvider = new UpbitJwtProvider();

    @Test
    void toQueryStringKeepsRawValuesAndInsertionOrderForQueryHash() {
        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("state", "done");
        queryParams.put("start_time", "2026-06-27T00:00:00+09:00");
        queryParams.put("end_time", "2026-06-27T08:30:00+09:00");

        String queryString = upbitJwtProvider.toQueryString(queryParams);

        assertThat(queryString).isEqualTo(
                "state=done&start_time=2026-06-27T00:00:00+09:00&end_time=2026-06-27T08:30:00+09:00"
        );
    }

    @Test
    void createTokenHashesRawQueryStringInsteadOfUrlEncodedQueryString() {
        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("start_time", "2026-06-27T00:00:00+09:00");
        queryParams.put("end_time", "2026-06-27T08:30:00+09:00");
        String rawQueryString = "start_time=2026-06-27T00:00:00+09:00&end_time=2026-06-27T08:30:00+09:00";
        String encodedQueryString = "start_time=2026-06-27T00%3A00%3A00%2B09%3A00"
                + "&end_time=2026-06-27T08%3A30%3A00%2B09%3A00";

        String payload = decodePayload(upbitJwtProvider.createToken("access-key", "secret-key", queryParams));

        assertThat(payload).contains("\"query_hash\":\"" + sha512(rawQueryString) + "\"");
        assertThat(payload).doesNotContain(sha512(encodedQueryString));
    }

    private String decodePayload(String token) {
        String payload = token.split("\\.")[1];
        return new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
    }

    private String sha512(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-512")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte b : digest) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
