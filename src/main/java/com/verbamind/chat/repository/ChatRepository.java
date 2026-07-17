package com.verbamind.chat.repository;

import com.verbamind.chat.entity.Chat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChatRepository extends JpaRepository<Chat, UUID> {
    Page<Chat> findByOrganizationIdAndUserIdAndDeletedFalseOrderByUpdatedAtDesc(
            UUID organizationId, UUID userId, Pageable pageable);
}