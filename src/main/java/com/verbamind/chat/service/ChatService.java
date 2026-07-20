package com.verbamind.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.verbamind.ai.dto.AskQuestionResponse;
import com.verbamind.ai.dto.CitationDto;
import com.verbamind.ai.service.RagQueryService;
import com.verbamind.auth.entity.User;
import com.verbamind.auth.repository.UserRepository;
import com.verbamind.chat.dto.*;
import com.verbamind.chat.entity.Chat;
import com.verbamind.chat.entity.Message;
import com.verbamind.chat.entity.MessageRole;
import com.verbamind.chat.exception.ChatNotFoundException;
import com.verbamind.chat.repository.ChatRepository;
import com.verbamind.chat.repository.MessageRepository;
import com.verbamind.organization.entity.Organization;
import com.verbamind.organization.exception.OrganizationNotFoundException;
import com.verbamind.organization.repository.OrganizationRepository;
import com.verbamind.document.service.OrganizationAccessGuard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class ChatService {

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final OrganizationAccessGuard accessGuard;
    private final RagQueryService ragQueryService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ChatService(ChatRepository chatRepository,
                       MessageRepository messageRepository,
                       OrganizationRepository organizationRepository,
                       UserRepository userRepository,
                       OrganizationAccessGuard accessGuard,
                       RagQueryService ragQueryService) {
        this.chatRepository = chatRepository;
        this.messageRepository = messageRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.accessGuard = accessGuard;
        this.ragQueryService = ragQueryService;
    }

    @Transactional
    public ChatResponse createChat(UUID currentUserId, UUID organizationId, CreateChatRequest request) {
        accessGuard.requireMembership(organizationId, currentUserId);

        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException("Organization not found"));
        User user = userRepository.findById(currentUserId).orElseThrow();

        Chat chat = new Chat();
        chat.setOrganization(org);
        chat.setUser(user);
        chat.setTitle(request.title() != null && !request.title().isBlank() ? request.title() : "New Chat");
        chatRepository.save(chat);

        return toChatResponse(chat);
    }

    public ChatPageResponse listChats(UUID currentUserId, UUID organizationId, Pageable pageable) {
        accessGuard.requireMembership(organizationId, currentUserId);
        Page<Chat> page = chatRepository
                .findByOrganizationIdAndUserIdAndDeletedFalseOrderByUpdatedAtDesc(organizationId, currentUserId, pageable);
        List<ChatResponse> items = page.getContent().stream().map(this::toChatResponse).toList();
        return new ChatPageResponse(items, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    public ChatDetailResponse getChat(UUID currentUserId, UUID organizationId, UUID chatId) {
        accessGuard.requireMembership(organizationId, currentUserId);
        Chat chat = getOwnedChatOrThrow(organizationId, currentUserId, chatId);

        List<MessageResponse> messages = messageRepository
                .findByChatIdAndDeletedFalseOrderByCreatedAtAsc(chatId).stream()
                .map(this::toMessageResponse)
                .toList();

        return new ChatDetailResponse(chat.getId(), chat.getTitle(), messages);
    }


    @Transactional
    public MessageResponse sendMessage(UUID currentUserId, UUID organizationId, UUID chatId, SendMessageRequest request) {
        accessGuard.requireMembership(organizationId, currentUserId);
        Chat chat = getOwnedChatOrThrow(organizationId, currentUserId, chatId);

        Message userMessage = new Message();
        userMessage.setChat(chat);
        userMessage.setRole(MessageRole.USER);
        userMessage.setContent(request.content());
        messageRepository.save(userMessage);

        AskQuestionResponse ragResponse = ragQueryService.answer(organizationId, request.content());

        Message assistantMessage = new Message();
        assistantMessage.setChat(chat);
        assistantMessage.setRole(MessageRole.ASSISTANT);
        assistantMessage.setContent(ragResponse.answer());
        assistantMessage.setCitations(serializeCitations(ragResponse.citations()));
        messageRepository.saveAndFlush(assistantMessage);


        chat.setTitle(chat.getTitle());
        chatRepository.save(chat);

        return toMessageResponse(assistantMessage);
    }

    @Transactional
    public ChatResponse renameChat(UUID currentUserId, UUID organizationId, UUID chatId, RenameChatRequest request) {
        accessGuard.requireMembership(organizationId, currentUserId);
        Chat chat = getOwnedChatOrThrow(organizationId, currentUserId, chatId);
        chat.setTitle(request.title());
        chatRepository.save(chat);
        return toChatResponse(chat);
    }

    @Transactional
    public void deleteChat(UUID currentUserId, UUID organizationId, UUID chatId) {
        accessGuard.requireMembership(organizationId, currentUserId);
        Chat chat = getOwnedChatOrThrow(organizationId, currentUserId, chatId);
        chat.setDeleted(true);
        chatRepository.save(chat);
    }

    private Chat getOwnedChatOrThrow(UUID organizationId, UUID userId, UUID chatId) {
        Chat chat = chatRepository.findById(chatId)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ChatNotFoundException("Chat not found"));

        if (!chat.getOrganization().getId().equals(organizationId) || !chat.getUser().getId().equals(userId)) {
            throw new ChatNotFoundException("Chat not found");
        }
        return chat;
    }

    private String serializeCitations(List<CitationDto> citations) {
        try {
            return objectMapper.writeValueAsString(citations);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<CitationDto> deserializeCitations(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return Arrays.asList(objectMapper.readValue(json, CitationDto[].class));
        } catch (Exception e) {
            return List.of();
        }
    }

    private ChatResponse toChatResponse(Chat chat) {
        return new ChatResponse(chat.getId(), chat.getTitle(), chat.getCreatedAt(), chat.getUpdatedAt());
    }

    private MessageResponse toMessageResponse(Message message) {
        List<CitationDto> citations = message.getRole() == MessageRole.ASSISTANT
                ? deserializeCitations(message.getCitations())
                : List.of();
        return new MessageResponse(message.getId(), message.getRole(), message.getContent(),
                citations, message.getCreatedAt());
    }
}