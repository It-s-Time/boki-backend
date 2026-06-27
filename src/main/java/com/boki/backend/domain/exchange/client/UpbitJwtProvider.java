package com.boki.backend.domain.exchange.client;

import com.boki.backend.domain.exchange.exception.ExchangeErrorCode;
import com.boki.backend.global.apiPayload.exception.GeneralException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class UpbitJwtProvider {

    private static final String HMAC_SHA512 = "HmacSHA512";

    public String createToken(String accessKey, String secretKey) {
        return createToken(accessKey, secretKey, Map.of());
    }

    public String createToken(String accessKey, String secretKey, Map<String, String> queryParams) {
        String header = "{\"alg\":\"HS512\",\"typ\":\"JWT\"}";
        String payload = createPayload(accessKey, queryParams);
        String unsignedToken = base64Url(header.getBytes(StandardCharsets.UTF_8))
                + "."
                + base64Url(payload.getBytes(StandardCharsets.UTF_8));

        return unsignedToken + "." + sign(unsignedToken, secretKey);
    }

    public String toQueryString(Map<String, String> queryParams) {
        return queryParams.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
    }

    public String toEncodedQueryString(Map<String, String> queryParams) {
        return queryParams.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String createPayload(String accessKey, Map<String, String> queryParams) {
        StringBuilder payload = new StringBuilder()
                .append("{")
                .append("\"access_key\":\"").append(accessKey).append("\",")
                .append("\"nonce\":\"").append(UUID.randomUUID()).append("\"");

        if (!queryParams.isEmpty()) {
            payload.append(",")
                    .append("\"query_hash\":\"").append(sha512(toQueryString(queryParams))).append("\",")
                    .append("\"query_hash_alg\":\"SHA512\"");
        }

        return payload.append("}").toString();
    }

    private String sign(String value, String secretKey) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA512);
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA512));
            return base64Url(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new GeneralException(ExchangeErrorCode.API_REQUEST_FAILED);
        }
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
            throw new GeneralException(ExchangeErrorCode.API_REQUEST_FAILED);
        }
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
