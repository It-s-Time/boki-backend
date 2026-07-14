package com.boki.backend.domain.member.service;

import org.springframework.web.multipart.MultipartFile;

public interface MemberImageStorage {

    String upload(MultipartFile file, Long memberId);
}
