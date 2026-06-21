package com.boki.backend.global.storage.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "boki.aws.s3", name = "enabled", havingValue = "true")
public class S3CleanupSchedulingConfig {
}
