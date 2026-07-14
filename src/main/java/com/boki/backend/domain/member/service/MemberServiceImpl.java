package com.boki.backend.domain.member.service;

import com.boki.backend.domain.auth.service.RefreshTokenService;
import com.boki.backend.domain.member.dto.request.MemberUpdateRequest;
import com.boki.backend.domain.member.dto.response.MemberResponse;
import com.boki.backend.domain.member.entity.Member;
import com.boki.backend.domain.member.exception.MemberErrorCode;
import com.boki.backend.domain.member.repository.MemberRepository;
import com.boki.backend.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final RefreshTokenService refreshTokenService;

    @Override
    public MemberResponse getMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(MemberErrorCode.MEMBER_NOT_FOUND));
        return MemberResponse.from(member);
    }

    @Override
    @Transactional
    public MemberResponse updateMember(Long memberId, MemberUpdateRequest request, MultipartFile profileImage) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(MemberErrorCode.MEMBER_NOT_FOUND));
        member.update(request.nickname(), null);
        return MemberResponse.from(member);
    }

    @Override
    @Transactional
    public void withdrawMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(MemberErrorCode.MEMBER_NOT_FOUND));
        refreshTokenService.delete(memberId);
        memberRepository.delete(member);
    }
}
