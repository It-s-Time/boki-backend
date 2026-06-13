package com.boki.backend.domain.review.service;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface ReviewImageStorage {

    ReviewImageUploadResult upload(MultipartFile file, Long memberId, Long reviewId, int orderIndex);

    void deleteAll(List<String> objectKeys);
}
