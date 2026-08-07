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
@ConditionalOnProperty(prefix = "verbamind.ai", name = "provider", havingValue = "groq")
public class GroqProvider implements AiProvider {

    private static final String BASE_URL = "https://api.groq.com/openai/v1";

    private final AiProperties properties;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    public GroqProvider(AiProperties properties) {
        this.properties = properties;
    }

    @Override
    @Timed(value = "ai.embedding.latency", extraTags = {"provider", "groq"})
    public float[] generateEmbedding(String text) {
        return generateEmbeddings(List.of(text)).get(0);
    }

    @Override
    @Timed(value = "ai.embedding.latency", extraTags = {"provider", "groq"})
    public List<float[]> generateEmbeddings(List<String> texts) {
        try {
            String body = mapper.writeValueAsString(java.util.Map.of(
                    "model", properties.getGroq().getEmbeddingModel(),
                    "input", texts
            ));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/embeddings"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + properties.getGroq().getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode json = mapper.readTree(res.body());

            if (json.has("error")) {
                throw new AiProviderException("Groq embedding request failed: " + json.get("error").get("message").asText());
            }

            List<float[]> vectors = new ArrayList<>();
            for (JsonNode item : json.get("data")) {
                JsonNode arr = item.get("embedding");
                float[] v = new float[arr.size()];
                for (int i = 0; i < arr.size(); i++) v[i] = (float) arr.get(i).asDouble();
                vectors.add(v);
            }
            return vectors;
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new AiProviderException("Groq embedding request failed: " + e.getMessage());
        }
    }

    @Override
    @Timed(value = "ai.completion.latency", extraTags = {"provider", "groq", "streaming", "false"})
    public String generateCompletion(String systemPrompt, String userPrompt) {
        try {
            String body = mapper.writeValueAsString(java.util.Map.of(
                    "model", properties.getGroq().getChatModel(),
                    "messages", List.of(
                            java.util.Map.of("role", "system", "content", systemPrompt),
                            java.util.Map.of("role", "user", "content", userPrompt)
                    )
            ));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + properties.getGroq().getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode json = mapper.readTree(res.body());

            if (json.has("error")) {
                throw new AiProviderException("Groq completion request failed: " + json.get("error").get("message").asText());
            }

            JsonNode choices = json.get("choices");
            if (choices == null || !choices.isArray() || choices.isEmpty()) {
                throw new AiProviderException(
                        "Groq completion request failed: unexpected response (HTTP " + res.statusCode() + "): " + res.body());
            }

            return choices.get(0).get("message").get("content").asText();
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new AiProviderException("Groq completion request failed: " + e.getMessage());
        }
    }

    @Override
    @Timed(value = "ai.completion.latency", extraTags = {"provider", "groq", "streaming", "true"})
    public void generateCompletionStream(String systemPrompt, String userPrompt,
                                         java.util.function.Consumer<String> onToken,
                                         Runnable onComplete) {
        try {
            String body = mapper.writeValueAsString(java.util.Map.of(
                    "model", properties.getGroq().getChatModel(),
                    "stream", true,
                    "messages", List.of(
                            java.util.Map.of("role", "system", "content", systemPrompt),
                            java.util.Map.of("role", "user", "content", userPrompt)
                    )
            ));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + properties.getGroq().getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<java.io.InputStream> res = httpClient.send(req, HttpResponse.BodyHandlers.ofInputStream());

            if (res.statusCode() >= 400) {
                String errorBody = new String(res.body().readAllBytes());
                throw new AiProviderException(
                        "Groq completion request failed (HTTP " + res.statusCode() + "): " + errorBody);
            }

            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(res.body()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    if (!line.startsWith("data:")) continue;

                    String data = line.substring(5).trim();
                    if (data.equals("[DONE]")) break;

                    JsonNode json = mapper.readTree(data);

                    if (json.has("error")) {
                        throw new AiProviderException("Groq completion request failed: " + json.get("error").get("message").asText());
                    }

                    JsonNode choices = json.get("choices");
                    if (choices == null || !choices.isArray() || choices.isEmpty()) continue;

                    JsonNode delta = choices.get(0).get("delta");
                    if (delta != null && delta.has("content") && !delta.get("content").isNull()) {
                        String token = delta.get("content").asText();
                        if (!token.isEmpty()) {
                            onToken.accept(token);
                        }
                    }
                }
            }
            onComplete.run();
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new AiProviderException("Groq completion request failed: " + e.getMessage());
        }
    }
}