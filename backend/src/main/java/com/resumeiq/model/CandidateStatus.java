package com.resumeiq.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "candidate_status")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CandidateStatus {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private JobRole jobRole;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    private LocalDateTime updatedAt;

    @PrePersist @PreUpdate
    public void prePersist() { this.updatedAt = LocalDateTime.now(); }

    public enum Status { APPLIED, UNDER_REVIEW, SELECTED, WAITLISTED, REJECTED }
}
