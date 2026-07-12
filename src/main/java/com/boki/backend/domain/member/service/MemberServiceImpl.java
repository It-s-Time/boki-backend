package com.boki.backend.domain.member.service;

import com.boki.backend.domain.member.dto.response.MemberResponse;
import com.boki.backend.domain.member.entity.Member;
import com.boki.backend.domain.member.exception.MemberErrorCode;
import com.boki.backend.domain.member.repository.MemberRepository;
import com.boki.backend.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;

    @Override
    public MemberResponse getMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(MemberErrorCode.MEMBER_NOT_FOUND));
        return MemberResponse.from(member);
    }
}
