package com.boki.backend.domain.member.service;

import com.boki.backend.domain.member.dto.request.MemberUpdateRequest;
import com.boki.backend.domain.member.dto.response.MemberResponse;
import org.springframework.web.multipart.MultipartFile;

public interface MemberService {

    MemberResponse getMember(Long memberId);

    MemberResponse updateMember(Long memberId, MemberUpdateRequest request, MultipartFile profileImage);
}
