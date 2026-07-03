package com.boki.backend.domain.test.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.boki.backend.domain.auth.jwt.JwtTokenProvider;
import com.boki.backend.domain.auth.service.RefreshTokenService;
import com.boki.backend.domain.member.entity.Member;
import com.boki.backend.domain.member.entity.SocialProvider;
import com.boki.backend.domain.member.repository.MemberRepository;
import com.boki.backend.global.apiPayload.exception.handler.GlobalExceptionHandler;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(TestController.class)
class TestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @MockitoBean
    private MemberRepository memberRepository;

    @Test
    void successReturnsApiResponse() throws Exception {
        mockMvc.perform(get("/api/test/success"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess", is(true)))
                .andExpect(jsonPath("$.code", is("COMMON200")))
                .andExpect(jsonPath("$.message", is("성공입니다.")))
                .andExpect(jsonPath("$.result", is("test success")));
    }

    @Test
    void failureReturnsApiResponse() throws Exception {
        mockMvc.perform(get("/api/test/failure"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess", is(false)))
                .andExpect(jsonPath("$.code", is("COMMON400")))
                .andExpect(jsonPath("$.message", is("잘못된 요청입니다.")))
                .andExpect(jsonPath("$.result").doesNotExist());
    }

    @Test
    void issueTokenReturnsJwtPair() throws Exception {
        Member member = Member.builder()
                .email("test-1@test.com")
                .provider(SocialProvider.KAKAO)
                .providerId("test-user-provider-id-1")
                .build();
        setMemberId(member, 1L);
        when(memberRepository.findById(1L)).thenReturn(Optional.empty());
        when(memberRepository.save(org.mockito.ArgumentMatchers.any(Member.class))).thenReturn(member);
        when(jwtTokenProvider.createAccessToken(1L)).thenReturn("access-token");
        when(jwtTokenProvider.createRefreshToken(1L)).thenReturn("refresh-token");
        when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(1000L);

        mockMvc.perform(post("/api/test/token")
                        .param("memberId", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess", is(true)))
                .andExpect(jsonPath("$.result.memberId", is(1)))
                .andExpect(jsonPath("$.result.accessToken", is("access-token")))
                .andExpect(jsonPath("$.result.refreshToken", is("refresh-token")));

        verify(refreshTokenService).save(1L, "refresh-token", 1000L);
    }

    @Test
    void healthReturnsUp() throws Exception {
        mockMvc.perform(get("/api/test/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }

    private void setMemberId(Member member, Long memberId) throws Exception {
        var field = Member.class.getDeclaredField("memberId");
        field.setAccessible(true);
        field.set(member, memberId);
    }
}
