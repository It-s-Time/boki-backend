package com.boki.backend.domain.auth.service;

import com.boki.backend.domain.auth.client.OAuthClient;
import com.boki.backend.domain.auth.client.OAuthUserInfo;
import com.boki.backend.domain.auth.dto.response.AuthTokenResponse;
import com.boki.backend.domain.auth.exception.AuthErrorCode;
import com.boki.backend.domain.auth.jwt.JwtTokenProvider;
import com.boki.backend.domain.member.entity.Member;
import com.boki.backend.domain.member.entity.SocialProvider;
import com.boki.backend.domain.member.repository.MemberRepository;
import com.boki.backend.global.apiPayload.exception.GeneralException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SocialLoginServiceImpl implements SocialLoginService {

    private final List<OAuthClient> oauthClients;
    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Override
    public String getAuthorizationUri(SocialProvider provider) {
        return findClient(provider).getAuthorizationUri();
    }

    @Override
    @Transactional
    public AuthTokenResponse login(SocialProvider provider, String code) {
        OAuthUserInfo userInfo = findClient(provider).getUserInfo(code);
        Member member = memberRepository.findByProviderAndProviderId(userInfo.provider(), userInfo.providerId())
                .orElseGet(() -> memberRepository.save(Member.builder()
                        .email(userInfo.email())
                        .provider(userInfo.provider())
                        .providerId(userInfo.providerId())
                        .build()));

        return issueTokens(member);
    }

    @Override
    public AuthTokenResponse reissue(String refreshToken) {
        Long memberId = jwtTokenProvider.getMemberIdFromRefreshToken(refreshToken);
        refreshTokenService.validate(memberId, refreshToken);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(AuthErrorCode.INVALID_TOKEN));

        String accessToken = jwtTokenProvider.createAccessToken(member.getMemberId());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(member.getMemberId());
        refreshTokenService.save(
                member.getMemberId(),
                newRefreshToken,
                jwtTokenProvider.getRefreshTokenExpirationMs()
        );

        return new AuthTokenResponse(
                member.getMemberId(),
                member.getEmail(),
                member.getProvider(),
                accessToken,
                newRefreshToken
        );
    }

    @Override
    public void logout(String refreshToken) {
        Long memberId = jwtTokenProvider.getMemberIdFromRefreshToken(refreshToken);
        refreshTokenService.delete(memberId);
    }

    private AuthTokenResponse issueTokens(Member member) {
        String accessToken = jwtTokenProvider.createAccessToken(member.getMemberId());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getMemberId());
        refreshTokenService.save(member.getMemberId(), refreshToken, jwtTokenProvider.getRefreshTokenExpirationMs());

        return new AuthTokenResponse(
                member.getMemberId(),
                member.getEmail(),
                member.getProvider(),
                accessToken,
                refreshToken
        );
    }

    private OAuthClient findClient(SocialProvider provider) {
        return oauthClients.stream()
                .filter(client -> client.supports(provider))
                .findFirst()
                .orElseThrow(() -> new GeneralException(AuthErrorCode.UNSUPPORTED_PROVIDER));
    }
}
