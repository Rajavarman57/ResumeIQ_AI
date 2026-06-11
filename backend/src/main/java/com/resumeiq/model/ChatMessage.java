package com.resumeiq.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatMessage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long resumeId;

    @Column(nullable = false)
    private Long jobId;

    @Enumerated(EnumType.STRING)
    private Sender sender;   // USER or AI

    @Column(columnDefinition = "CLOB", nullable = false)
    private String content;

    private LocalDateTime sentAt;

    @PrePersist
    public void prePersist() { this.sentAt = LocalDateTime.now(); }

    public enum Sender { USER, AI }
}
