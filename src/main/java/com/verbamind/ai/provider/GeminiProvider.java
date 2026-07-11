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
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "verbamind.ai", name = "provider", havingValue = "gemini")
public class GeminiProvider implements AiProvider {

    private final AiProperties properties;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    public GeminiProvider(AiProperties properties) {
        this.properties = properties;
    }

    @Override
    public float[] generateEmbedding(String text) {
        try {
            String body = mapper.writeValueAsString(Map.of(
                    "model", "models/text-embedding-004",
                    "content", Map.of("parts", List.of(Map.of("text", text)))
            ));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/text-embedding-004:embedContent?key="
                            + properties.getGemini().getApiKey()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode json = mapper.readTree(res.body());
            JsonNode arr = json.get("embedding").get("values");
            float[] vector = new float[arr.size()];
            for (int i = 0; i < arr.size(); i++) vector[i] = (float) arr.get(i).asDouble();
            return vector;
        } catch (Exception e) {
            throw new AiProviderException("Gemini embedding request failed: " + e.getMessage());
        }
    }

    @Override
    public List<float[]> generateEmbeddings(List<String> texts) {
        List<float[]> results = new ArrayList<>();
        for (String text : texts) results.add(generateEmbedding(text)); // batch via loop; swap for batchEmbedContents if needed later
        return results;
    }

    @Override
    public String generateCompletion(String systemPrompt, String userPrompt) {
        try {
            String model = properties.getGemini().getChatModel();
            String body = mapper.writeValueAsString(Map.of(
                    "systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))),
                    "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", userPrompt))))
            ));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/" + model
                            + ":generateContent?key=" + properties.getGemini().getApiKey()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode json = mapper.readTree(res.body());
            return json.get("candidates").get(0).get("content").get("parts").get(0).get("text").asText();
        } catch (Exception e) {
            throw new AiProviderException("Gemini completion request failed: " + e.getMessage());
        }
    }
}