package com.boki.backend.domain.member.dto.response;

import com.boki.backend.domain.member.entity.Member;

public record MemberResponse(
        Long memberId,
        String nickname,
        String profileImageUrl
) {
    public static MemberResponse from(Member member) {
        String nickname = member.getNickname() != null ? member.getNickname() : "김보키";
        return new MemberResponse(
                member.getMemberId(),
                nickname,
                member.getProfileImageUrl()
        );
    }
}
