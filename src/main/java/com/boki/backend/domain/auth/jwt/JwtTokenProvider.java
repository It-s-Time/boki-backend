package com.boki.backend.domain.auth.jwt;

import com.boki.backend.domain.auth.config.JwtProperties;
import com.boki.backend.domain.auth.exception.AuthErrorCode;
import com.boki.backend.global.apiPayload.exception.GeneralException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String REFRESH_TOKEN_TYPE = "REFRESH";

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Long memberId) {
        return createToken(memberId, ACCESS_TOKEN_TYPE, jwtProperties.accessTokenExpirationMs());
    }

    public String createRefreshToken(Long memberId) {
        return createToken(memberId, REFRESH_TOKEN_TYPE, jwtProperties.refreshTokenExpirationMs());
    }

    public Long getMemberIdFromAccessToken(String token) {
        Claims claims = parseClaims(token);
        validateTokenType(claims, ACCESS_TOKEN_TYPE);
        return Long.parseLong(claims.getSubject());
    }

    public Long getMemberIdFromRefreshToken(String token) {
        Claims claims = parseClaims(token);
        validateTokenType(claims, REFRESH_TOKEN_TYPE);
        return Long.parseLong(claims.getSubject());
    }

    public long getRefreshTokenExpirationMs() {
        return jwtProperties.refreshTokenExpirationMs();
    }

    private String createToken(Long memberId, String tokenType, long expirationMs) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(signingKey)
                .compact();
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException exception) {
            throw new GeneralException(AuthErrorCode.EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new GeneralException(AuthErrorCode.INVALID_TOKEN);
        }
    }

    private void validateTokenType(Claims claims, String expectedTokenType) {
        if (!expectedTokenType.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) {
            throw new GeneralException(AuthErrorCode.INVALID_TOKEN);
        }
    }
}
