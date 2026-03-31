package com.snehil.project.lovable_clone.repository;

import com.snehil.project.lovable_clone.entity.ChatSession;
import com.snehil.project.lovable_clone.entity.ChatSessionId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatSessionRepository extends JpaRepository<ChatSession, ChatSessionId> {
}
