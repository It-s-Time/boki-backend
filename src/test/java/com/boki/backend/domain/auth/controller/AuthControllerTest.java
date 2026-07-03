package com.boki.backend.domain.auth.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.boki.backend.domain.auth.dto.response.AuthTokenResponse;
import com.boki.backend.domain.auth.service.SocialLoginService;
import com.boki.backend.domain.member.entity.SocialProvider;
import com.boki.backend.global.apiPayload.exception.handler.GlobalExceptionHandler;
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
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SocialLoginService socialLoginService;

    @Test
    void redirectToProviderReturnsProviderAuthorizationUri() throws Exception {
        when(socialLoginService.getAuthorizationUri(SocialProvider.KAKAO, "boki://auth/callback"))
                .thenReturn("https://kauth.kakao.com/oauth/authorize?state=state");

        mockMvc.perform(get("/auth/oauth2/kakao")
                        .param("redirectUri", "boki://auth/callback"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://kauth.kakao.com/oauth/authorize?state=state"));
    }

    @Test
    void oauthCallbackRedirectsToAppWithLoginCode() throws Exception {
        AuthTokenResponse tokenResponse = new AuthTokenResponse(
                1L,
                "test@test.com",
                SocialProvider.KAKAO,
                "access-token",
                "refresh-token"
        );
        when(socialLoginService.consumeRedirectUri("state")).thenReturn("boki://auth/callback");
        when(socialLoginService.login(SocialProvider.KAKAO, "code")).thenReturn(tokenResponse);
        when(socialLoginService.createLoginCode(tokenResponse)).thenReturn("login-code");

        mockMvc.perform(get("/auth/oauth2/kakao/callback")
                        .param("code", "code")
                        .param("state", "state"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "boki://auth/callback?loginCode=login-code"));
    }

    @Test
    void oauthCallbackRedirectsToAppWithErrorWhenProviderReturnsError() throws Exception {
        when(socialLoginService.consumeRedirectUri("state")).thenReturn("boki://auth/callback");

        mockMvc.perform(get("/auth/oauth2/kakao/callback")
                        .param("error", "access_denied")
                        .param("state", "state"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "boki://auth/callback?error=oauth_failed"));
    }

    @Test
    void exchangeLoginCodeReturnsTokenResponse() throws Exception {
        AuthTokenResponse tokenResponse = new AuthTokenResponse(
                1L,
                "test@test.com",
                SocialProvider.KAKAO,
                "access-token",
                "refresh-token"
        );
        when(socialLoginService.exchangeLoginCode("login-code")).thenReturn(tokenResponse);

        mockMvc.perform(post("/auth/oauth2/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginCode": "login-code"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess", is(true)))
                .andExpect(jsonPath("$.result.memberId", is(1)))
                .andExpect(jsonPath("$.result.provider", is("KAKAO")))
                .andExpect(jsonPath("$.result.accessToken", is("access-token")))
                .andExpect(jsonPath("$.result.refreshToken", is("refresh-token")));

        verify(socialLoginService).exchangeLoginCode("login-code");
    }
}
