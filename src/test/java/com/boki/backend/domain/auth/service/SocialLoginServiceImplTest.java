package com.boki.backend.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.boki.backend.domain.auth.dto.response.AuthTokenResponse;
import com.boki.backend.domain.auth.jwt.JwtTokenProvider;
import com.boki.backend.domain.member.entity.Member;
import com.boki.backend.domain.member.entity.SocialProvider;
import com.boki.backend.domain.member.repository.MemberRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SocialLoginServiceImplTest {

    private SocialLoginServiceImpl socialLoginService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        socialLoginService = new SocialLoginServiceImpl(
                List.of(),
                memberRepository,
                jwtTokenProvider,
                refreshTokenService
        );
    }

    @Test
    void reissueRotatesRefreshTokenAndUpdatesRedis() throws Exception {
        Member member = Member.builder()
                .email("test@test.com")
                .provider(SocialProvider.KAKAO)
                .providerId("provider-id")
                .build();
        setMemberId(member, 1L);

        when(jwtTokenProvider.getMemberIdFromRefreshToken("old-refresh-token")).thenReturn(1L);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(jwtTokenProvider.createAccessToken(1L)).thenReturn("new-access-token");
        when(jwtTokenProvider.createRefreshToken(1L)).thenReturn("new-refresh-token");
        when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(1000L);

        AuthTokenResponse response = socialLoginService.reissue("old-refresh-token");

        assertThat(response.memberId()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("test@test.com");
        assertThat(response.provider()).isEqualTo(SocialProvider.KAKAO);
        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
        verify(refreshTokenService).validate(1L, "old-refresh-token");
        verify(refreshTokenService).save(1L, "new-refresh-token", 1000L);
    }

    private void setMemberId(Member member, Long memberId) throws Exception {
        var field = Member.class.getDeclaredField("memberId");
        field.setAccessible(true);
        field.set(member, memberId);
    }
}
