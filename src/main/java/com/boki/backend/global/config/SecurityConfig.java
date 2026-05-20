package com.boki.backend.global.config;

import com.boki.backend.domain.auth.jwt.JwtAuthenticationFilter;
import com.boki.backend.domain.auth.jwt.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            ObjectProvider<JwtTokenProvider> jwtTokenProviderProvider,
            ObjectMapper objectMapper
    ) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/auth/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/actuator/health",
                        "/actuator/info"
                ).permitAll()
                // 개발 단계에서는 기존 X-User-Id/userId=1 fallback 흐름을 유지합니다.
                .anyRequest().permitAll());
        jwtTokenProviderProvider.ifAvailable(jwtTokenProvider ->
                http.addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider, objectMapper),
                        UsernamePasswordAuthenticationFilter.class
                ));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
