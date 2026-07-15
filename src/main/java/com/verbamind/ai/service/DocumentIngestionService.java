package com.verbamind.ai.service;

import com.pgvector.PGvector;
import com.verbamind.ai.entity.DocumentChunk;
import com.verbamind.ai.provider.AiProvider;
import com.verbamind.ai.repository.DocumentChunkRepository;
import com.verbamind.document.entity.Document;
import com.verbamind.document.entity.DocumentStatus;
import com.verbamind.document.repository.DocumentRepository;
import com.verbamind.document.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final StorageService storageService;
    private final TextExtractionService textExtractionService;
    private final ChunkingService chunkingService;
    private final AiProvider aiProvider;

    public DocumentIngestionService(DocumentRepository documentRepository,
                                    DocumentChunkRepository chunkRepository,
                                    StorageService storageService,
                                    TextExtractionService textExtractionService,
                                    ChunkingService chunkingService,
                                    AiProvider aiProvider) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.storageService = storageService;
        this.textExtractionService = textExtractionService;
        this.chunkingService = chunkingService;
        this.aiProvider = aiProvider;
    }


    @Async
    @Transactional
    public void processDocument(UUID documentId) {
        Document doc = documentRepository.findById(documentId).orElse(null);
        if (doc == null) return;

        try {
            doc.setStatus(DocumentStatus.PROCESSING);
            documentRepository.save(doc);

            String text;
            try (InputStream stream = storageService.download(doc.getStorageKey())) {
                text = textExtractionService.extract(stream);
            }

            List<String> chunks = chunkingService.chunk(text);
            if (chunks.isEmpty()) {
                doc.setStatus(DocumentStatus.FAILED);
                documentRepository.save(doc);
                return;
            }

            List<float[]> embeddings = aiProvider.generateEmbeddings(chunks);

            for (int i = 0; i < chunks.size(); i++) {
                DocumentChunk chunk = new DocumentChunk();
                chunk.setDocumentId(doc.getId());
                chunk.setOrganizationId(doc.getOrganization().getId());
                chunk.setChunkIndex(i);
                chunk.setContent(chunks.get(i));
                chunk.setEmbedding(new PGvector(embeddings.get(i)));
                chunkRepository.save(chunk);
            }

            doc.setStatus(DocumentStatus.READY);
            documentRepository.save(doc);

        } catch (Exception e) {
            log.error("Ingestion failed for document {}: {}", documentId, e.getMessage(), e);
            doc.setStatus(DocumentStatus.FAILED);
            documentRepository.save(doc);
        }
    }

    @Transactional
    public void deleteEmbeddings(UUID documentId) {
        chunkRepository.deleteByDocumentId(documentId);
    }
}