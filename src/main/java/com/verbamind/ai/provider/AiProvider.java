package com.verbamind.ai.provider;

import java.util.List;

public interface AiProvider {
    float[] generateEmbedding(String text);
    String generateCompletion(String systemPrompt, String userPrompt);
    List<float[]> generateEmbeddings(List<String> texts);
    void generateCompletionStream(String systemPrompt, String userPrompt,
                                  java.util.function.Consumer<String> onToken,
                                  Runnable onComplete);
}
