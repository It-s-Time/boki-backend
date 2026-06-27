package com.boki.backend.domain.exchange.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(UpbitTradeSyncProperties.class)
public class ExchangeConfig {
}
