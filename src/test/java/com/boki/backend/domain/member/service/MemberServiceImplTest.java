package com.boki.backend.domain.member.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.boki.backend.domain.auth.service.RefreshTokenService;
import com.boki.backend.domain.member.entity.Member;
import com.boki.backend.domain.member.entity.SocialProvider;
import com.boki.backend.domain.member.exception.MemberErrorCode;
import com.boki.backend.domain.member.repository.MemberRepository;
import com.boki.backend.global.apiPayload.exception.GeneralException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberServiceImplTest {

    private MemberServiceImpl memberService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        memberService = new MemberServiceImpl(memberRepository, refreshTokenService);
    }

    @Test
    void withdrawMemberDeletesRefreshTokenAndMember() {
        Member member = Member.builder()
                .email("test@test.com")
                .provider(SocialProvider.KAKAO)
                .providerId("provider-id")
                .build();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        memberService.withdrawMember(1L);

        verify(refreshTokenService).delete(1L);
        verify(memberRepository).delete(member);
    }

    @Test
    void withdrawMemberThrowsWhenMemberDoesNotExist() {
        when(memberRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.withdrawMember(1L))
                .isInstanceOf(GeneralException.class)
                .extracting("code")
                .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND);
        verify(refreshTokenService, never()).delete(anyLong());
    }
}
