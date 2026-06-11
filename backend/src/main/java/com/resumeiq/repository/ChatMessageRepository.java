package com.resumeiq.repository;

import com.resumeiq.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByResumeIdAndJobIdOrderBySentAtAsc(Long resumeId, Long jobId);
    void deleteByResumeIdAndJobId(Long resumeId, Long jobId);
}
