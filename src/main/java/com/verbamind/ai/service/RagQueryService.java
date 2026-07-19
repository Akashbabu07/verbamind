package com.verbamind.ai.service;

import com.pgvector.PGvector;
import com.verbamind.ai.dto.AskQuestionRequest;
import com.verbamind.ai.dto.AskQuestionResponse;
import com.verbamind.ai.dto.CitationDto;
import com.verbamind.ai.entity.DocumentChunk;
import com.verbamind.ai.provider.AiProvider;
import com.verbamind.ai.repository.DocumentChunkRepository;
import com.verbamind.document.repository.DocumentRepository;
import com.verbamind.document.service.OrganizationAccessGuard;
import com.verbamind.usage.service.UsageService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RagQueryService {

    private static final int TOP_K = 5;

    private final DocumentChunkRepository chunkRepository;
    private final DocumentRepository documentRepository;
    private final AiProvider aiProvider;
    private final OrganizationAccessGuard accessGuard;
    private final UsageService usageService;

    public RagQueryService(DocumentChunkRepository chunkRepository,
                           DocumentRepository documentRepository,
                           AiProvider aiProvider,
                           OrganizationAccessGuard accessGuard,
                           UsageService usageService) {
        this.chunkRepository = chunkRepository;
        this.documentRepository = documentRepository;
        this.aiProvider = aiProvider;
        this.accessGuard = accessGuard;
        this.usageService = usageService;
    }


    public AskQuestionResponse answer(UUID organizationId, String question) {
        usageService.reserveAiRequest(organizationId);

        float[] questionEmbedding = aiProvider.generateEmbedding(question);
        String vectorLiteral = toVectorLiteral(questionEmbedding);

        List<DocumentChunk> relevantChunks =
                chunkRepository.findSimilarChunks(organizationId, vectorLiteral, TOP_K);

        if (relevantChunks.isEmpty()) {

            return new AskQuestionResponse(
                    "I don't have any relevant documents to answer that question yet.",
                    List.of());
        }

        String context = buildContext(relevantChunks);
        String systemPrompt = """
            You are verbamind, an assistant that answers questions strictly using the
            provided document excerpts. Always cite which excerpt(s) support each
            claim using [1], [2], etc. If the excerpts don't contain the answer,
            say so honestly instead of guessing.
            """;
        String userPrompt = "Context:\n" + context + "\n\nQuestion: " + question;

        String answer = aiProvider.generateCompletion(systemPrompt, userPrompt);

        long approxTokens = estimateTokens(userPrompt) + estimateTokens(answer);
        usageService.addTokensUsed(organizationId, approxTokens);

        List<CitationDto> citations = buildCitations(relevantChunks);

        return new AskQuestionResponse(answer, citations);
    }


    private long estimateTokens(String text) {
        return text == null ? 0 : Math.max(1, text.length() / 4);
    }

    public AskQuestionResponse ask(UUID currentUserId, UUID organizationId, AskQuestionRequest request) {
        accessGuard.requireMembership(organizationId, currentUserId);
        return answer(organizationId, request.question());
    }

    private String buildContext(List<DocumentChunk> chunks) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            sb.append("[").append(i + 1).append("] ").append(chunks.get(i).getContent()).append("\n\n");
        }
        return sb.toString();
    }

    private List<CitationDto> buildCitations(List<DocumentChunk> chunks) {
        // cache document filenames to avoid N+1 lookups when multiple chunks share a document
        Map<UUID, String> fileNameCache = new LinkedHashMap<>();
        List<CitationDto> citations = new java.util.ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);
            String fileName = fileNameCache.computeIfAbsent(chunk.getDocumentId(),
                    id -> documentRepository.findById(id).map(d -> d.getFileName()).orElse("Unknown document"));

            citations.add(new CitationDto(i + 1, chunk.getDocumentId(), fileName,
                    chunk.getChunkIndex(), snippet(chunk.getContent())));
        }
        return citations;
    }

    private String snippet(String content) {
        return content.length() > 200 ? content.substring(0, 200) + "..." : content;
    }

    private String toVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        return sb.append("]").toString();
    }
}