package com.boki.backend.domain.member.controller;

import com.boki.backend.domain.member.dto.request.MemberUpdateRequest;
import com.boki.backend.domain.member.dto.response.MemberResponse;
import com.boki.backend.domain.member.service.MemberService;
import com.boki.backend.global.apiPayload.ApiResponse;
import com.boki.backend.global.apiPayload.code.GeneralSuccessCode;
import com.boki.backend.global.auth.AuthenticatedUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Member", description = "회원 API")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;
    private final AuthenticatedUserProvider userProvider;

    @Operation(summary = "내 프로필 조회", description = "현재 로그인한 사용자의 닉네임, 프로필 이미지를 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberResponse>> getMyProfile() {
        return ResponseEntity
                .status(GeneralSuccessCode.OK.getStatus())
                .body(ApiResponse.onSuccess(GeneralSuccessCode.OK, memberService.getMember(userProvider.getCurrentUserId())));
    }

    @Operation(summary = "내 프로필 수정", description = "닉네임과 프로필 이미지를 수정합니다.")
    @PatchMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<MemberResponse>> updateMyProfile(
            @Size(max = 50, message = "닉네임은 50자 이하여야 합니다.") @RequestPart(value = "nickname", required = false) String nickname,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage
    ) {
        MemberUpdateRequest request = new MemberUpdateRequest(nickname);
        return ResponseEntity
                .status(GeneralSuccessCode.OK.getStatus())
                .body(ApiResponse.onSuccess(GeneralSuccessCode.OK, memberService.updateMember(userProvider.getCurrentUserId(), request, profileImage)));
    }

    @Operation(summary = "회원 탈퇴", description = "현재 로그인한 사용자의 회원 정보를 삭제하고 Redis에 저장된 refresh token을 삭제합니다.")
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdrawMyAccount() {
        memberService.withdrawMember(userProvider.getCurrentUserId());
        return ResponseEntity
                .status(GeneralSuccessCode.DELETED.getStatus())
                .body(ApiResponse.onSuccess(GeneralSuccessCode.DELETED, null));
    }
}
