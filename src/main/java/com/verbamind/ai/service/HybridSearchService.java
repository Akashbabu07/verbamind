package com.verbamind.ai.service;

import io.micrometer.core.annotation.Timed;
import com.verbamind.ai.entity.DocumentChunk;
import com.verbamind.ai.repository.DocumentChunkRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class HybridSearchService {

    private static final int RRF_K = 60;

    private final DocumentChunkRepository chunkRepository;

    public HybridSearchService(DocumentChunkRepository chunkRepository) {
        this.chunkRepository = chunkRepository;
    }

    @Timed(value = "ai.hybrid_search.latency")
    public List<DocumentChunk> search(UUID organizationId, String query, String vectorLiteral, int topK) {
        int poolSize = topK * 4;

        List<DocumentChunk> vectorResults = chunkRepository.findSimilarChunks(organizationId, vectorLiteral, poolSize);
        List<DocumentChunk> keywordResults = chunkRepository.findByFullTextSearch(organizationId, query, poolSize);

        Map<UUID, Double> fusedScores = new LinkedHashMap<>();
        Map<UUID, DocumentChunk> chunksById = new LinkedHashMap<>();

        addRankScores(vectorResults, fusedScores, chunksById);
        addRankScores(keywordResults, fusedScores, chunksById);

        return fusedScores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topK)
                .map(e -> chunksById.get(e.getKey()))
                .collect(Collectors.toList());
    }

    private void addRankScores(List<DocumentChunk> results, Map<UUID, Double> fusedScores, Map<UUID, DocumentChunk> chunksById) {
        for (int i = 0; i < results.size(); i++) {
            DocumentChunk chunk = results.get(i);
            int rank = i + 1;
            double rrfScore = 1.0 / (RRF_K + rank);
            fusedScores.merge(chunk.getId(), rrfScore, Double::sum);
            chunksById.putIfAbsent(chunk.getId(), chunk);
        }
    }
}