package com.verbamind.ai.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChunkingServiceTest {

    private final ChunkingService chunkingService = new ChunkingService();

    @Test
    void chunk_returnsEmptyList_forNullOrBlankInput() {
        assertThat(chunkingService.chunk(null)).isEmpty();
        assertThat(chunkingService.chunk("   ")).isEmpty();
        assertThat(chunkingService.chunk("")).isEmpty();
    }

    @Test
    void chunk_returnsSingleChunk_whenTextShorterThanChunkSize() {
        String text = "This is a short document about refund policies.";

        List<String> chunks = chunkingService.chunk(text);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).isEqualTo(text);
    }

    @Test
    void chunk_splitsLongTextIntoOverlappingChunks() {
        // 2500 chars, well beyond the 1000-char chunk size
        String text = "word ".repeat(500).trim();

        List<String> chunks = chunkingService.chunk(text);

        assertThat(chunks.size()).isGreaterThan(1);
        // Every chunk except possibly the last should be exactly CHUNK_SIZE (1000) chars
        for (int i = 0; i < chunks.size() - 1; i++) {
            assertThat(chunks.get(i)).hasSize(1000);
        }
         String tailOfFirst = chunks.get(0).substring(chunks.get(0).length() - 150);
        String headOfSecond = chunks.get(1).substring(0, 150);
        assertThat(headOfSecond).isEqualTo(tailOfFirst);
    }

    @Test
    void chunk_collapsesWhitespace() {
        String messy = "This   has\n\nirregular\t\tspacing.";

        List<String> chunks = chunkingService.chunk(messy);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).isEqualTo("This has irregular spacing.");
    }
}
