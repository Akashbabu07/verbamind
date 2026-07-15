package com.verbamind.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "verbamind.ai")
public class AiProperties {

    @Setter
    private String provider;

    private final Ollama ollama = new Ollama();
    private  final OpenAi openai = new OpenAi();
    private  final Gemini gemini = new Gemini();
    private  final Claude claude = new Claude();

    public String getProvider() { return provider; }

    public Ollama getOllama() { return ollama; }
    public OpenAi getOpenai() { return openai; }
    public Gemini getGemini() { return gemini; }
    public Claude getClaude() { return claude; }

    @Setter
    @Getter
    public static class Ollama {
        private String baseUrl;
        private String embeddingModel = "nomic-embed-text";
        private String chatModel = "llama3.2";

    }

    @Setter
    @Getter
    public static class OpenAi {
        private String apiKey;
        private String embeddingModel = "text-embedding-3-small";
        private String chatModel = "gpt-4o-mini";

    }

    @Getter
    @Setter
    public static class Gemini {
        private String apiKey;
        private String chatModel = "gemini-1.5-flash";
    }

    @Getter
    @Setter
    public static class Claude {
        private String apiKey;
        private String chatModel = "claude-sonnet-4-6";

    }
}