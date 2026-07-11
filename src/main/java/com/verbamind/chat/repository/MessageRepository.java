package com.verbamind.chat.repository;

import com.verbamind.chat.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findByChatIdAndDeletedFalseOrderByCreatedAtAsc(UUID chatId);
}