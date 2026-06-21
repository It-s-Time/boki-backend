package com.boki.backend.domain.ai.client;

import com.boki.backend.domain.ai.exception.AiReportErrorCode;
import com.boki.backend.global.apiPayload.exception.GeneralException;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OpenAiClient {

    private final RestClient openAiRestClient;
    private final String model;

    public OpenAiClient(
            @Qualifier("openAiRestClient") RestClient openAiRestClient,
            @Value("${openai.model}") String model
    ) {
        this.openAiRestClient = openAiRestClient;
        this.model = model;
    }

    public String requestCompletion(String systemPrompt, String userPrompt) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "response_format", Map.of("type", "json_object"),
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    )
            );

            Map<?, ?> response = openAiRestClient.post()
                    .uri("/v1/chat/completions")
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            List<?> choices = (List<?>) response.get("choices");
            Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
            Map<?, ?> message = (Map<?, ?>) firstChoice.get("message");
            return (String) message.get("content");
        } catch (Exception e) {
            throw new GeneralException(AiReportErrorCode.AI_API_CALL_FAILED);
        }
    }
}
