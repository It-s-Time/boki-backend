package com.boki.backend.domain.member.service;

import com.boki.backend.domain.member.exception.MemberErrorCode;
import com.boki.backend.global.apiPayload.exception.GeneralException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@ConditionalOnExpression("!'${boki.aws.s3.enabled:false}'.equalsIgnoreCase('true')")
public class DisabledMemberImageStorage implements MemberImageStorage {

    @PostConstruct
    void logStorageMode() {
        log.warn("Member image storage is DISABLED. Set AWS_S3_ENABLED=true to upload profile images to S3.");
    }

    @Override
    public String upload(MultipartFile file, Long memberId) {
        log.warn("프로필 이미지 업로드 요청이 들어왔지만 S3가 비활성화 상태입니다. memberId={}", memberId);
        throw new GeneralException(MemberErrorCode.MEMBER_IMAGE_UPLOAD_FAILED);
    }
}
