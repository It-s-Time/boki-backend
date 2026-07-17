package com.boki.backend.domain.member.service;

import com.boki.backend.domain.member.exception.MemberErrorCode;
import com.boki.backend.global.apiPayload.exception.GeneralException;
import com.boki.backend.global.config.S3Properties;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "boki.aws.s3", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class S3MemberImageStorage implements MemberImageStorage {

    private final S3Client s3Client;
    private final S3Properties properties;

    @PostConstruct
    void logStorageMode() {
        log.info("Member image storage is S3. bucket={}, region={}", properties.bucket(), properties.region());
    }

    @Override
    public String upload(MultipartFile file, Long memberId) {
        String objectKey = "profiles/%d/%s%s".formatted(memberId, UUID.randomUUID(), extensionFor(file));

        try (var inputStream = file.getInputStream()) {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(properties.bucket())
                            .key(objectKey)
                            .contentType(file.getContentType())
                            .contentLength(file.getSize())
                            .build(),
                    RequestBody.fromInputStream(inputStream, file.getSize())
            );
        } catch (IOException | SdkException e) {
            log.warn("프로필 이미지 S3 업로드 실패. bucket={}, key={}", properties.bucket(), objectKey, e);
            throw new GeneralException(MemberErrorCode.MEMBER_IMAGE_UPLOAD_FAILED);
        }

        return buildImageUrl(objectKey);
    }

    private String extensionFor(MultipartFile file) {
        String contentType = file.getContentType();
        if ("image/jpeg".equals(contentType)) return ".jpg";
        if ("image/png".equals(contentType)) return ".png";
        if ("image/webp".equals(contentType)) return ".webp";
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
    }

    private String buildImageUrl(String objectKey) {
        if (properties.publicBaseUrl() != null && !properties.publicBaseUrl().isBlank()) {
            return properties.publicBaseUrl().replaceAll("/+$", "") + "/" + objectKey;
        }
        return "https://%s.s3.%s.amazonaws.com/%s".formatted(
                properties.bucket(), properties.region(), objectKey
        );
    }
}
