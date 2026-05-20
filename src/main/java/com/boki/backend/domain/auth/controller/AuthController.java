package com.boki.backend.domain.auth.controller;

import com.boki.backend.domain.auth.dto.request.LogoutRequest;
import com.boki.backend.domain.auth.dto.request.TokenReissueRequest;
import com.boki.backend.domain.auth.dto.response.AuthTokenResponse;
import com.boki.backend.domain.auth.service.SocialLoginService;
import com.boki.backend.domain.member.entity.SocialProvider;
import com.boki.backend.global.apiPayload.ApiResponse;
import com.boki.backend.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증", description = "소셜 로그인 및 토큰 관리 API")
@Validated
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SocialLoginService socialLoginService;

    @Operation(summary = "소셜 로그인", description = "provider 인가 페이지로 redirect합니다. provider: google, kakao")
    @GetMapping("/oauth2/{provider}")
    public ResponseEntity<Void> redirectToProvider(@PathVariable String provider) {
        String authorizationUri = socialLoginService.getAuthorizationUri(SocialProvider.from(provider));
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(authorizationUri))
                .build();
    }

    @Operation(summary = "OAuth2 콜백", description = "provider authorization code로 회원 생성/조회 후 토큰을 발급합니다.")
    @GetMapping("/oauth2/{provider}/callback")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> oauthCallback(
            @PathVariable String provider,
            @RequestParam @NotBlank(message = "code는 필수입니다.") String code
    ) {
        AuthTokenResponse response = socialLoginService.login(SocialProvider.from(provider), code);
        return ResponseEntity
                .status(GeneralSuccessCode.OK.getStatus())
                .body(ApiResponse.onSuccess(GeneralSuccessCode.OK, response));
    }

    @Operation(summary = "토큰 재발급", description = "refresh token을 검증하고 access token을 재발급합니다.")
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> reissue(
            @RequestBody @Valid TokenReissueRequest request
    ) {
        AuthTokenResponse response = socialLoginService.reissue(request.refreshToken());
        return ResponseEntity
                .status(GeneralSuccessCode.OK.getStatus())
                .body(ApiResponse.onSuccess(GeneralSuccessCode.OK, response));
    }

    @Operation(summary = "로그아웃", description = "Redis에 저장된 refresh token을 삭제합니다.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody @Valid LogoutRequest request
    ) {
        socialLoginService.logout(request.refreshToken());
        return ResponseEntity
                .status(GeneralSuccessCode.OK.getStatus())
                .body(ApiResponse.onSuccess(GeneralSuccessCode.OK, null));
    }
}
