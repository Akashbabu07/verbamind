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
@ConditionalOnProperty(prefix = "verbamind.ai", name = "provider", havingValue = "ollama")
public class OllamaProvider implements AiProvider {

    private final AiProperties properties;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    public OllamaProvider(AiProperties properties) {
        this.properties = properties;
    }

    @Override
    public float[] generateEmbedding(String text) {
        try {
            String body = mapper.writeValueAsString(java.util.Map.of(
                    "model", properties.getOllama().getEmbeddingModel(),
                    "prompt", text
            ));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getOllama().getBaseUrl() + "/api/embeddings"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode json = mapper.readTree(res.body());
            JsonNode arr = json.get("embedding");
            float[] vector = new float[arr.size()];
            for (int i = 0; i < arr.size(); i++) vector[i] = (float) arr.get(i).asDouble();
            return vector;
        } catch (Exception e) {
            throw new AiProviderException("Ollama embedding request failed: " + e.getMessage());
        }
    }

    @Override
    public List<float[]> generateEmbeddings(List<String> texts) {
        List<float[]> results = new ArrayList<>();
        for (String text : texts) results.add(generateEmbedding(text)); // Ollama has no native batch endpoint
        return results;
    }

    @Override
    public String generateCompletion(String systemPrompt, String userPrompt) {
        try {
            String body = mapper.writeValueAsString(java.util.Map.of(
                    "model", properties.getOllama().getChatModel(),
                    "stream", false,
                    "messages", List.of(
                            java.util.Map.of("role", "system", "content", systemPrompt),
                            java.util.Map.of("role", "user", "content", userPrompt)
                    )
            ));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getOllama().getBaseUrl() + "/api/chat"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode json = mapper.readTree(res.body());
            return json.get("message").get("content").asText();
        } catch (Exception e) {
            throw new AiProviderException("Ollama completion request failed: " + e.getMessage());
        }
    }
}