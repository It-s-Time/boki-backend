package com.boki.backend.domain.test.controller;

import com.boki.backend.domain.auth.jwt.JwtTokenProvider;
import com.boki.backend.domain.auth.service.RefreshTokenService;
import com.boki.backend.domain.member.entity.Member;
import com.boki.backend.domain.member.entity.SocialProvider;
import com.boki.backend.domain.member.repository.MemberRepository;
import com.boki.backend.global.apiPayload.ApiResponse;
import com.boki.backend.global.apiPayload.code.GeneralErrorCode;
import com.boki.backend.global.apiPayload.code.GeneralSuccessCode;
import com.boki.backend.global.apiPayload.exception.GeneralException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Test", description = "공통 응답 및 예외 처리 테스트 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test")
public class TestController {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final MemberRepository memberRepository;

    @Operation(summary = "공통 성공 응답 테스트", description = "ApiResponse 성공 응답 형식을 확인합니다.")
    @GetMapping("/success")
    public ResponseEntity<ApiResponse<String>> success() {
        return ResponseEntity
                .status(GeneralSuccessCode.OK.getStatus())
                .body(ApiResponse.onSuccess(GeneralSuccessCode.OK, "test success"));
    }

    @Operation(summary = "공통 실패 응답 테스트", description = "GlobalExceptionHandler 실패 응답 형식을 확인합니다.")
    @GetMapping("/failure")
    public ResponseEntity<ApiResponse<Void>> failure() {
        throw new GeneralException(GeneralErrorCode.BAD_REQUEST);
    }

    @Operation(summary = "헬스 체크", description = "서버가 정상적으로 실행 중인지 확인합니다.")
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(new HealthResponse("UP", Instant.now()));
    }

    @Operation(summary = "테스트 유저 토큰 발급", description = "테스트 Member가 없으면 생성하고 로컬 개발/수동 테스트용 JWT를 발급합니다.")
    @PostMapping("/token")
    public ResponseEntity<ApiResponse<TestTokenResponse>> issueToken(
            @RequestParam @Positive(message = "memberId는 0보다 커야 합니다.") Long memberId
    ) {
        Member member = memberRepository.findById(memberId)
                .orElseGet(() -> memberRepository.save(Member.builder()
                        .email("test-" + memberId + "@test.com")
                        .provider(SocialProvider.KAKAO)
                        .providerId("test-user-provider-id-" + memberId)
                        .build()));
        String accessToken = jwtTokenProvider.createAccessToken(member.getMemberId());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getMemberId());
        refreshTokenService.save(member.getMemberId(), refreshToken, jwtTokenProvider.getRefreshTokenExpirationMs());

        return ResponseEntity
                .status(GeneralSuccessCode.OK.getStatus())
                .body(ApiResponse.onSuccess(
                        GeneralSuccessCode.OK,
                        new TestTokenResponse(member.getMemberId(), accessToken, refreshToken)
                ));
    }

    public record TestTokenResponse(
            Long memberId,
            String accessToken,
            String refreshToken
    ) {
    }

    public record HealthResponse(String status, Instant timestamp) {
    }
}
