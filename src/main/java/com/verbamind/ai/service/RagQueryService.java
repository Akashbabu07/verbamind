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

    public RagQueryService(DocumentChunkRepository chunkRepository,
                           DocumentRepository documentRepository,
                           AiProvider aiProvider,
                           OrganizationAccessGuard accessGuard) {
        this.chunkRepository = chunkRepository;
        this.documentRepository = documentRepository;
        this.aiProvider = aiProvider;
        this.accessGuard = accessGuard;
    }

    // add this method to the existing RagQueryService class

    /**
     * Core RAG call without membership/quota concerns — used internally by
     * ChatService, which already owns those checks at the chat level.
     */
    public AskQuestionResponse answer(UUID organizationId, String question) {
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
            You are DocuMind, an assistant that answers questions strictly using the
            provided document excerpts. Always cite which excerpt(s) support each
            claim using [1], [2], etc. If the excerpts don't contain the answer,
            say so honestly instead of guessing.
            """;
        String userPrompt = "Context:\n" + context + "\n\nQuestion: " + question;

        String answer = aiProvider.generateCompletion(systemPrompt, userPrompt);
        List<CitationDto> citations = buildCitations(relevantChunks);

        return new AskQuestionResponse(answer, citations);
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