package com.boki.backend.domain.exchange.config;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(UpbitTradeSyncProperties.class)
public class ExchangeConfig {

    private static final String UPBIT_BASE_URL = "https://api.upbit.com";

    @Bean
    public RestClient upbitApiRestClient(RestClient.Builder builder) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(3));

        return builder
                .baseUrl(UPBIT_BASE_URL)
                .requestFactory(requestFactory)
                .build();
    }

    @Bean(destroyMethod = "close")
    public ExecutorService upbitPermissionValidationExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
