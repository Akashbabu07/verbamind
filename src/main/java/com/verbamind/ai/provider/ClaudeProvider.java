package com.verbamind.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.verbamind.ai.config.AiProperties;
import com.verbamind.ai.exception.AiProviderException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "verbamind.ai", name = "provider", havingValue = "claude")
public class ClaudeProvider implements AiProvider {

    private final AiProperties properties;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    public ClaudeProvider(AiProperties properties) {
        this.properties = properties;
    }

    @Override
    public float[] generateEmbedding(String text) {
        throw new AiProviderException(
                "Claude has no native embeddings API. Set verbamind.ai.embeddings-provider " +
                        "to 'openai' or 'ollama' while keeping provider=claude for chat completions.");
    }

    @Override
    public List<float[]> generateEmbeddings(List<String> texts) {
        throw new AiProviderException(
                "Claude has no native embeddings API. Set verbamind.ai.embeddings-provider " +
                        "to 'openai' or 'ollama' while keeping provider=claude for chat completions.");
    }

    @Override
    public String generateCompletion(String systemPrompt, String userPrompt) {
        try {
            String body = mapper.writeValueAsString(Map.of(
                    "model", properties.getClaude().getChatModel(),
                    "max_tokens", 1024,
                    "system", systemPrompt,
                    "messages", List.of(Map.of("role", "user", "content", userPrompt))
            ));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.anthropic.com/v1/messages"))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", properties.getClaude().getApiKey())
                    .header("anthropic-version", "2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode json = mapper.readTree(res.body());
            return json.get("content").get(0).get("text").asText();
        } catch (Exception e) {
            throw new AiProviderException("Claude completion request failed: " + e.getMessage());
        }
    }
}