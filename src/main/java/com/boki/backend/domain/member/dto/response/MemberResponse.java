package com.boki.backend.domain.member.dto.response;

import com.boki.backend.domain.member.entity.Member;

public record MemberResponse(
        Long memberId,
        String nickname,
        String profileImageUrl
) {
    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getMemberId(),
                member.getNickname(),
                member.getProfileImageUrl()
        );
    }
}
