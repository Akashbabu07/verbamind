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
import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "verbamind.ai", name = "provider", havingValue = "openai")
public class OpenAiProvider implements AiProvider {

    private final AiProperties properties;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    public OpenAiProvider(AiProperties properties) {
        this.properties = properties;
    }

    @Override
    public float[] generateEmbedding(String text) {
        return generateEmbeddings(List.of(text)).get(0);
    }

    @Override
    public List<float[]> generateEmbeddings(List<String> texts) {
        try {
            String body = mapper.writeValueAsString(java.util.Map.of(
                    "model", properties.getOpenai().getEmbeddingModel(),
                    "input", texts
            ));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/embeddings"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + properties.getOpenai().getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode json = mapper.readTree(res.body());
            List<float[]> vectors = new ArrayList<>();
            for (JsonNode item : json.get("data")) {
                JsonNode arr = item.get("embedding");
                float[] v = new float[arr.size()];
                for (int i = 0; i < arr.size(); i++) v[i] = (float) arr.get(i).asDouble();
                vectors.add(v);
            }
            return vectors;
        } catch (Exception e) {
            throw new AiProviderException("OpenAI embedding request failed: " + e.getMessage());
        }
    }

    @Override
    public String generateCompletion(String systemPrompt, String userPrompt) {
        try {
            String body = mapper.writeValueAsString(java.util.Map.of(
                    "model", properties.getOpenai().getChatModel(),
                    "messages", List.of(
                            java.util.Map.of("role", "system", "content", systemPrompt),
                            java.util.Map.of("role", "user", "content", userPrompt)
                    )
            ));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + properties.getOpenai().getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode json = mapper.readTree(res.body());
            return json.get("choices").get(0).get("message").get("content").asText();
        } catch (Exception e) {
            throw new AiProviderException("OpenAI completion request failed: " + e.getMessage());
        }
    }
}