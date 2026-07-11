package com.verbamind.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "verbamind.ai")
public class AiProperties {

    private String provider; // ollama | openai | gemini | claude

    private Ollama ollama = new Ollama();
    private OpenAi openai = new OpenAi();
    private Gemini gemini = new Gemini();
    private Claude claude = new Claude();

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public Ollama getOllama() { return ollama; }
    public OpenAi getOpenai() { return openai; }
    public Gemini getGemini() { return gemini; }
    public Claude getClaude() { return claude; }

    public static class Ollama {
        private String baseUrl;
        private String embeddingModel = "nomic-embed-text";
        private String chatModel = "llama3.1";
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getEmbeddingModel() { return embeddingModel; }
        public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }
        public String getChatModel() { return chatModel; }
        public void setChatModel(String chatModel) { this.chatModel = chatModel; }
    }

    public static class OpenAi {
        private String apiKey;
        private String embeddingModel = "text-embedding-3-small";
        private String chatModel = "gpt-4o-mini";
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getEmbeddingModel() { return embeddingModel; }
        public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }
        public String getChatModel() { return chatModel; }
        public void setChatModel(String chatModel) { this.chatModel = chatModel; }
    }

    public static class Gemini {
        private String apiKey;
        private String chatModel = "gemini-1.5-flash";
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getChatModel() { return chatModel; }
        public void setChatModel(String chatModel) { this.chatModel = chatModel; }
    }

    public static class Claude {
        private String apiKey;
        private String chatModel = "claude-sonnet-4-6";
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getChatModel() { return chatModel; }
        public void setChatModel(String chatModel) { this.chatModel = chatModel; }
    }
}