package com.verbamind.chat.controller;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.verbamind.chat.dto.*;
import com.verbamind.chat.service.ChatService;
import com.verbamind.common.dto.ApiResponse;
import com.verbamind.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/organizations/{organizationId}/chats")
public class ChatController {

    private final ChatService chatService;
    private final java.util.concurrent.ExecutorService chatStreamExecutor;

    public ChatController(ChatService chatService, java.util.concurrent.ExecutorService chatStreamExecutor) {
        this.chatService = chatService;
        this.chatStreamExecutor = chatStreamExecutor;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ChatResponse>> create(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID organizationId,
            @RequestBody(required = false) CreateChatRequest request) {
        var body = request != null ? request : new CreateChatRequest(null);
        var chat = chatService.createChat(currentUser.getId(), organizationId, body);
        return ResponseEntity.ok(ApiResponse.success(chat, "Chat created"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ChatPageResponse>> list(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID organizationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(
                chatService.listChats(currentUser.getId(), organizationId, pageable)));
    }

    @GetMapping("/{chatId}")
    public ResponseEntity<ApiResponse<ChatDetailResponse>> get(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID organizationId,
            @PathVariable UUID chatId) {
        return ResponseEntity.ok(ApiResponse.success(
                chatService.getChat(currentUser.getId(), organizationId, chatId)));
    }

    @PostMapping("/{chatId}/messages")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID organizationId,
            @PathVariable UUID chatId,
            @Valid @RequestBody SendMessageRequest request) {
        var message = chatService.sendMessage(currentUser.getId(), organizationId, chatId, request);
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    @PatchMapping("/{chatId}")
    public ResponseEntity<ApiResponse<ChatResponse>> rename(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID organizationId,
            @PathVariable UUID chatId,
            @Valid @RequestBody RenameChatRequest request) {
        var chat = chatService.renameChat(currentUser.getId(), organizationId, chatId, request);
        return ResponseEntity.ok(ApiResponse.success(chat, "Chat renamed"));
    }

    @DeleteMapping("/{chatId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID organizationId,
            @PathVariable UUID chatId) {
        chatService.deleteChat(currentUser.getId(), organizationId, chatId);
        return ResponseEntity.ok(ApiResponse.success(null, "Chat deleted"));
    }

//    @PostMapping(value = "/{chatId}/messages/stream", produces = "text/event-stream")
//    public SseEmitter sendMessageStream(
//            @AuthenticationPrincipal CustomUserDetails currentUser,
//            @PathVariable UUID organizationId,
//            @PathVariable UUID chatId,
//            @Valid @RequestBody SendMessageRequest request) {
//
//        SseEmitter emitter = new SseEmitter(60_000L);
//        chatStreamExecutor.submit(() -> {
//            try {
//                chatService.streamMessage(currentUser.getId(), organizationId, chatId, request,
//                        token -> {
//                            try {
//                                emitter.send(SseEmitter.event().name("token").data(token));
//                            } catch (Exception e) {
//                                emitter.completeWithError(e);
//                            }
//                        },
//                        () -> {
//                            try {
//                                emitter.send(SseEmitter.event().name("done").data(""));
//                                emitter.complete();
//                            } catch (Exception e) {
//                                emitter.completeWithError(e);
//                            }
//                        });
//            } catch (Exception e) {
//                emitter.completeWithError(e);
//            }
//        });
//
//        return emitter;
//    }

    @PostMapping(value = "/{chatId}/messages/stream", produces = "text/event-stream")
    public SseEmitter sendMessageStream(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID organizationId,
            @PathVariable UUID chatId,
            @Valid @RequestBody SendMessageRequest request) {

        SseEmitter emitter = new SseEmitter(60_000L);

        if (currentUser == null) {
            emitter.completeWithError(new org.springframework.security.access.AccessDeniedException("Unauthenticated"));
            return emitter;
        }

        chatStreamExecutor.submit(() -> {
            try {
                chatService.streamMessage(currentUser.getId(), organizationId, chatId, request,
                        token -> {
                            try {
                                emitter.send(SseEmitter.event().name("token").data(token));
                            } catch (Exception e) {
                                emitter.completeWithError(e);
                            }
                        },
                        () -> {
                            try {
                                emitter.send(SseEmitter.event().name("done").data(""));
                                emitter.complete();
                            } catch (Exception e) {
                                emitter.completeWithError(e);
                            }
                        });
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}