package com.boki.backend.domain.test.controller;

import com.boki.backend.global.apiPayload.ApiResponse;
import com.boki.backend.global.apiPayload.code.GeneralErrorCode;
import com.boki.backend.global.apiPayload.code.GeneralSuccessCode;
import com.boki.backend.global.apiPayload.exception.GeneralException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Test", description = "공통 응답 및 예외 처리 테스트 API")
@RestController
@RequestMapping("/api/test")
public class TestController {

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
}
