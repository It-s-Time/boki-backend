package com.boki.backend.domain.exchange.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "boki.exchange.upbit.sync")
public record UpbitTradeSyncProperties(
        boolean enabled,
        long fixedDelayMs,
        long intervalHours,
        long retryDelayMinutes,
        long staleLockMinutes,
        int batchSize
) {
}
