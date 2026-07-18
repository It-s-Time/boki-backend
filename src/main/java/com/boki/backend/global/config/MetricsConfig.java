package com.boki.backend.global.config;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    private static final String REDACTED = "[REDACTED]";
    private static final List<String> SENSITIVE_TAG_KEYS = List.of("uri", "path", "url", "endpoint");
    private static final Pattern SENSITIVE_KEY_VALUE = Pattern.compile(
            "(?i).*(access[_-]?key|api[_-]?key|secret|token|password|authorization|bearer)=.+"
    );
    private static final Pattern BEARER_TOKEN = Pattern.compile("(?i).*bearer\\s+[a-z0-9._~+/=-]{16,}.*");
    private static final Pattern JWT = Pattern.compile(".*eyJ[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+.*");

    @Bean
    public MeterFilter redactSensitiveMetricTagValues() {
        return new MeterFilter() {
            @Override
            public Meter.Id map(Meter.Id id) {
                List<Tag> tags = id.getTags().stream()
                        .map(MetricsConfig::redactSensitiveTag)
                        .toList();
                return id.replaceTags(tags);
            }
        };
    }

    private static Tag redactSensitiveTag(Tag tag) {
        if (!SENSITIVE_TAG_KEYS.contains(tag.getKey())) {
            return tag;
        }
        return Tag.of(tag.getKey(), redactIfSensitive(tag.getValue()));
    }

    private static String redactIfSensitive(String tagValue) {
        if (tagValue == null || tagValue.isBlank()) {
            return tagValue;
        }
        if (SENSITIVE_KEY_VALUE.matcher(tagValue).matches()
                || BEARER_TOKEN.matcher(tagValue).matches()
                || JWT.matcher(tagValue).matches()) {
            return REDACTED;
        }
        return tagValue;
    }
}
