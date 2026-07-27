package com.verbamind.ai.provider;

import io.micrometer.core.annotation.Timed;
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
    @Timed(value = "ai.embedding.latency", extraTags = {"provider", "ollama"})
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

            if (json.has("error")) {
                throw new AiProviderException("Ollama embedding request failed: " + json.get("error").asText());
            }

            JsonNode arr = json.get("embedding");
            if (arr == null || !arr.isArray()) {
                throw new AiProviderException(
                        "Ollama embedding request failed: unexpected response (HTTP " + res.statusCode() + "): " + res.body());
            }

            float[] vector = new float[arr.size()];
            for (int i = 0; i < arr.size(); i++) vector[i] = (float) arr.get(i).asDouble();
            return vector;
        } catch (Exception e) {
            throw new AiProviderException("Ollama embedding request failed: " + e.getMessage());
        }
    }

    @Override
    @Timed(value = "ai.embedding.latency", extraTags = {"provider", "ollama"})
    public List<float[]> generateEmbeddings(List<String> texts) {
        List<float[]> results = new ArrayList<>();
        for (String text : texts) results.add(generateEmbedding(text));
        return results;
    }

    @Override
    @Timed(value = "ai.completion.latency", extraTags = {"provider", "ollama", "streaming", "false"})
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

            if (json.has("error")) {
                throw new AiProviderException("Ollama completion request failed: " + json.get("error").asText());
            }

            JsonNode message = json.get("message");
            if (message == null || message.get("content") == null) {
                throw new AiProviderException(
                        "Ollama completion request failed: unexpected response (HTTP " + res.statusCode() + "): " + res.body());
            }

            return message.get("content").asText();
        } catch (Exception e) {
            throw new AiProviderException("Ollama completion request failed: " + e.getMessage());
        }
    }
    @Override
    @Timed(value = "ai.completion.latency", extraTags = {"provider", "ollama", "streaming", "true"})
    public void generateCompletionStream(String systemPrompt, String userPrompt,
                                         java.util.function.Consumer<String> onToken,
                                         Runnable onComplete) {
        try {
            String body = mapper.writeValueAsString(java.util.Map.of(
                    "model", properties.getOllama().getChatModel(),
                    "stream", true,
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

            HttpResponse<java.io.InputStream> res = httpClient.send(req, HttpResponse.BodyHandlers.ofInputStream());

            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(res.body()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    JsonNode json = mapper.readTree(line);

                    if (json.has("error")) {
                        throw new AiProviderException("Ollama completion request failed: " + json.get("error").asText());
                    }

                    JsonNode message = json.get("message");
                    if (message != null && message.get("content") != null) {
                        String token = message.get("content").asText();
                        if (!token.isEmpty()) {
                            onToken.accept(token);
                        }
                    }

                    if (json.has("done") && json.get("done").asBoolean()) {
                        break;
                    }
                }
            }
            onComplete.run();
        } catch (Exception e) {
            throw new AiProviderException("Ollama completion request failed: " + e.getMessage());
        }
    }
}