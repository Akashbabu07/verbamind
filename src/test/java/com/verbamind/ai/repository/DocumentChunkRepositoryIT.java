package com.verbamind.ai.repository;

import com.verbamind.ai.entity.DocumentChunk;
import com.pgvector.PGvector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the real Flyway migrations against a real Postgres+pgvector container.
 * This is what actually proves the schema (V1..V14) is valid, and that the
 * native vector-similarity and full-text-search queries in
 * DocumentChunkRepository work against a real pgvector engine, not a mock.
 *
 * Requires Docker to be available wherever this test runs (CI runner or local machine).
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DocumentChunkRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("pgvector/pgvector:pg16")
                    .asCompatibleSubstituteFor("postgres"));

    @Autowired
    private DocumentChunkRepository chunkRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID organizationId;
    private UUID documentId;

    @BeforeEach
    void seedParentRows() {
        UUID userId = UUID.randomUUID();
        organizationId = UUID.randomUUID();
        documentId = UUID.randomUUID();

        jdbcTemplate.update("""
                INSERT INTO users (id, email, password, full_name)
                VALUES (?, ?, 'hashed', 'Test User')
                """, userId, "user-" + userId + "@example.com");

        jdbcTemplate.update("""
                INSERT INTO organizations (id, name, slug, owner_id)
                VALUES (?, 'Test Org', ?, ?)
                """, organizationId, "test-org-" + organizationId, userId);

        jdbcTemplate.update("""
                INSERT INTO documents (id, organization_id, owner_id, file_name, original_file_name,
                                        content_type, file_size, storage_key)
                VALUES (?, ?, ?, 'refunds.pdf', 'refunds.pdf', 'application/pdf', 1024, ?)
                """, documentId, organizationId, userId, "storage/" + documentId);
    }

    @Test
    void flywayMigrationsApplyCleanly_andChunkCanBeSavedAndRead() {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setDocumentId(documentId);
        chunk.setOrganizationId(organizationId);
        chunk.setChunkIndex(0);
        chunk.setContent("Refunds are available within 30 days of purchase.");
        chunk.setEmbedding(randomVector(768));

        DocumentChunk saved = chunkRepository.save(chunk);

        assertThat(saved.getId()).isNotNull();
        assertThat(chunkRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void vectorSimilaritySearch_returnsClosestChunkFirst() {
        DocumentChunk close = newChunk(0, "Refund policy details", nearVector(768, 0.01f));
        DocumentChunk far = newChunk(1, "Unrelated shipping info", nearVector(768, 5.0f));
        chunkRepository.save(close);
        chunkRepository.save(far);

        String queryVectorLiteral = toLiteral(nearVector(768, 0.0f));

        List<DocumentChunk> results =
                chunkRepository.findSimilarChunks(organizationId, queryVectorLiteral, 5);

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getContent()).isEqualTo("Refund policy details");
    }

    @Test
    void fullTextSearch_findsChunkByKeyword() {
        DocumentChunk chunk = newChunk(0, "Our refund window is thirty days from purchase.", randomVector(768));
        chunkRepository.save(chunk);

        List<DocumentChunk> results = chunkRepository.findByFullTextSearch(organizationId, "refund", 5);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getContent()).contains("refund");
    }

    private DocumentChunk newChunk(int index, String content, PGvector vector) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setDocumentId(documentId);
        chunk.setOrganizationId(organizationId);
        chunk.setChunkIndex(index);
        chunk.setContent(content);
        chunk.setEmbedding(vector);
        return chunk;
    }

    private PGvector randomVector(int dims) {
        float[] values = new float[dims];
        for (int i = 0; i < dims; i++) {
            values[i] = (float) Math.random();
        }
        return new PGvector(values);
    }

    /** A vector where every dimension equals `base`, useful for controlling distance in tests. */
    private PGvector nearVector(int dims, float base) {
        float[] values = new float[dims];
        java.util.Arrays.fill(values, base);
        return new PGvector(values);
    }

    private String toLiteral(PGvector vector) {
        return vector.toString();
    }
}
