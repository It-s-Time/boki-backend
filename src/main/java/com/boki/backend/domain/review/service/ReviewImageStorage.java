package com.boki.backend.domain.review.service;

import com.boki.backend.global.storage.service.S3ObjectStorage;
import org.springframework.web.multipart.MultipartFile;

public interface ReviewImageStorage extends S3ObjectStorage {

    ReviewImageUploadResult upload(MultipartFile file, Long memberId, Long reviewId, int orderIndex);
}
