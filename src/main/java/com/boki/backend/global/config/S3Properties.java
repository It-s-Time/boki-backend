package com.boki.backend.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "boki.aws.s3")
public record S3Properties(
        Boolean enabled,
        String region,
        String bucket,
        String publicBaseUrl,
        String accessKey,
        String secretKey
) {
}
