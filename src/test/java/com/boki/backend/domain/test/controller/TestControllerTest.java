package com.boki.backend.domain.test.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.boki.backend.global.apiPayload.exception.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(TestController.class)
class TestControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
}
