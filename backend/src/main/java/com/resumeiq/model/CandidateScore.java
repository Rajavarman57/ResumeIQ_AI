package com.resumeiq.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "candidate_scores")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CandidateScore {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private JobRole jobRole;

    private double matchScore;
    private double atsScore;

    @Column(columnDefinition = "TEXT")
    private String skillGap;

    @Column(columnDefinition = "TEXT")
    private String suggestions;

    @Column(columnDefinition = "TEXT")
    private String keywords;

    private LocalDateTime scoredAt;

    @PrePersist
    public void prePersist() { this.scoredAt = LocalDateTime.now(); }
}
