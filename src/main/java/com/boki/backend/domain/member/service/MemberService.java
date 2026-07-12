package com.boki.backend.domain.member.service;

import com.boki.backend.domain.member.dto.response.MemberResponse;

public interface MemberService {

    MemberResponse getMember(Long memberId);
}
