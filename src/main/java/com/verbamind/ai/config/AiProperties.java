package com.verbamind.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@ConfigurationProperties(prefix = "verbamind.ai")
public class AiProperties {

    @Setter
    private String provider;

    private final Ollama ollama = new Ollama();
    private  final OpenAi openai = new OpenAi();
    private  final Gemini gemini = new Gemini();
    private final Groq groq = new Groq();


    @Setter
    @Getter
    public static class Ollama {
        private String baseUrl;
        private String embeddingModel = "nomic-embed-text";
        private String chatModel = "llama3.2:3b";

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

    @Setter
    @Getter
    public static class Groq {
        private String apiKey;
        private String embeddingModel = "nomic-embed-text-v1_5";
        private String chatModel = "llama-3.3-70b-versatile";
    }

}