package com.verbamind.ai.service;

import com.verbamind.ai.dto.AskQuestionRequest;
import com.verbamind.ai.dto.AskQuestionResponse;
import com.verbamind.ai.entity.DocumentChunk;
import com.verbamind.ai.provider.AiProvider;
import com.verbamind.ai.repository.DocumentChunkRepository;
import com.verbamind.document.repository.DocumentRepository;
import com.verbamind.document.service.OrganizationAccessGuard;
import com.verbamind.usage.service.UsageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagQueryServiceTest {

    @Mock private DocumentChunkRepository chunkRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private AiProvider aiProvider;
    @Mock private OrganizationAccessGuard accessGuard;
    @Mock private UsageService usageService;
    @Mock private HybridSearchService hybridSearchService;

    @InjectMocks
    private RagQueryService ragQueryService;

    private final UUID orgId = UUID.randomUUID();

    @Test
    void answer_returnsFallbackMessage_whenNoRelevantChunksFound() {
        when(aiProvider.generateEmbedding(anyString())).thenReturn(new float[]{0.1f, 0.2f});
        when(hybridSearchService.search(eq(orgId), anyString(), anyString(), anyInt()))
                .thenReturn(List.of());

        AskQuestionResponse response = ragQueryService.answer(orgId, "What is the refund policy?");

        assertThat(response.answer()).contains("don't have any relevant documents");
        assertThat(response.citations()).isEmpty();
        verify(usageService).reserveAiRequest(orgId);
        // No completion should have been requested if there's no context to answer from
        verify(aiProvider, never()).generateCompletion(anyString(), anyString());
    }

    @Test
    void answer_callsProviderAndReturnsCitedAnswer_whenChunksFound() {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setDocumentId(UUID.randomUUID());
        chunk.setChunkIndex(0);
        chunk.setContent("Refunds are available within 30 days of purchase.");

        when(aiProvider.generateEmbedding(anyString())).thenReturn(new float[]{0.1f, 0.2f});
        when(hybridSearchService.search(eq(orgId), anyString(), anyString(), anyInt()))
                .thenReturn(List.of(chunk));
        when(aiProvider.generateCompletion(anyString(), anyString()))
                .thenReturn("You can get a refund within 30 days [1].");
        when(documentRepository.findById(chunk.getDocumentId())).thenReturn(java.util.Optional.empty());

        AskQuestionResponse response = ragQueryService.answer(orgId, "What is the refund policy?");

        assertThat(response.answer()).isEqualTo("You can get a refund within 30 days [1].");
        assertThat(response.citations()).hasSize(1);
        assertThat(response.citations().get(0).marker()).isEqualTo(1);

        verify(usageService).reserveAiRequest(orgId);
        verify(usageService).addTokensUsed(eq(orgId), anyLong());
        verify(aiProvider).generateCompletion(anyString(), anyString());
    }

    @Test
    void ask_checksMembershipBeforeAnswering() {
        UUID userId = UUID.randomUUID();
        when(aiProvider.generateEmbedding(anyString())).thenReturn(new float[]{0.1f});
        when(hybridSearchService.search(eq(orgId), anyString(), anyString(), anyInt()))
                .thenReturn(List.of());

        ragQueryService.ask(userId, orgId, new AskQuestionRequest("Any active discounts?"));

        verify(accessGuard).requireMembership(orgId, userId);
    }

    @Test
    void answerStream_streamsTokensAndInvokesOnCompleteWithFullAnswer() {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setDocumentId(UUID.randomUUID());
        chunk.setChunkIndex(0);
        chunk.setContent("Support is available 24/7 via chat.");

        when(aiProvider.generateEmbedding(anyString())).thenReturn(new float[]{0.1f});
        when(hybridSearchService.search(eq(orgId), anyString(), anyString(), anyInt()))
                .thenReturn(List.of(chunk));
        when(documentRepository.findById(chunk.getDocumentId())).thenReturn(java.util.Optional.empty());

        // Simulate the provider streaming three tokens then completing
        doAnswer(invocation -> {
            Consumer<String> onToken = invocation.getArgument(2);
            Runnable onProviderComplete = invocation.getArgument(3);
            onToken.accept("Support ");
            onToken.accept("is ");
            onToken.accept("24/7.");
            onProviderComplete.run();
            return null;
        }).when(aiProvider).generateCompletionStream(anyString(), anyString(), any(), any());

        StringBuilder streamed = new StringBuilder();
        StringBuilder finalAnswer = new StringBuilder();

        Consumer<String> onToken = streamed::append;
        BiConsumer<String, List<com.verbamind.ai.dto.CitationDto>> onComplete =
                (full, citations) -> finalAnswer.append(full);

        ragQueryService.answerStream(orgId, "How do I reach support?", onToken, onComplete);

        assertThat(streamed.toString()).isEqualTo("Support is 24/7.");
        assertThat(finalAnswer.toString()).isEqualTo("Support is 24/7.");
        verify(usageService).addTokensUsed(eq(orgId), anyLong());
    }
}
