package com.resumeiq.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "resumes")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Resume {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String candidateName;

    private String candidateEmail;
    private String candidatePhone;

    @Column(nullable = false)
    private String fileName;

    @Enumerated(EnumType.STRING)
    private FileType fileType;

    private String filePath;

    @Column(columnDefinition = "CLOB")
    private String rawText;

    @Column(columnDefinition = "TEXT")
    private String extractedSkills;

    private int experienceYears;

    @Column(columnDefinition = "TEXT")
    private String education;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    private LocalDateTime uploadedAt;

    @PrePersist
    public void prePersist() { this.uploadedAt = LocalDateTime.now(); }

    public enum FileType { PDF, DOCX }
}
