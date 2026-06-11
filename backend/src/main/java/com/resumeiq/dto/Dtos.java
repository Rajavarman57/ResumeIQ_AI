package com.resumeiq.dto;

import com.resumeiq.model.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

// ── Auth DTOs ────────────────────────────────────────────────
@Data @AllArgsConstructor @NoArgsConstructor
class LoginRequest { public String email; public String password; }

@Data @AllArgsConstructor @NoArgsConstructor
class AuthResponse { public String token; public String email; public String fullName; public String role; }

@Data @AllArgsConstructor @NoArgsConstructor
class RegisterRequest { public String email; public String password; public String fullName; public String role; }

// ── Job DTOs ─────────────────────────────────────────────────
@Data @AllArgsConstructor @NoArgsConstructor
class JobRequest {
    public String title;
    public String description;
    public String requiredSkills;
    public int experienceYears;
}

// ── Resume DTO ───────────────────────────────────────────────
@Data @AllArgsConstructor @NoArgsConstructor
class ResumeDto {
    public Long id;
    public String candidateName;
    public String candidateEmail;
    public String candidatePhone;
    public String fileName;
    public String fileType;
    public String extractedSkills;
    public int experienceYears;
    public String education;
    public LocalDateTime uploadedAt;
}

// ── Score DTO ────────────────────────────────────────────────
@Data @AllArgsConstructor @NoArgsConstructor
class ScoreDto {
    public Long scoreId;
    public Long resumeId;
    public String candidateName;
    public String candidateEmail;
    public Long jobId;
    public String jobTitle;
    public double matchScore;
    public double atsScore;
    public String skillGap;
    public String suggestions;
    public String keywords;
    public String status;
    public List<String> alternativeJobs;
}

// ── Status DTO ───────────────────────────────────────────────
@Data @AllArgsConstructor @NoArgsConstructor
class StatusRequest { public String status; public String notes; }

// ── Analytics DTO ────────────────────────────────────────────
@Data @AllArgsConstructor @NoArgsConstructor
class AnalyticsDto {
    public long totalResumes;
    public long totalJobs;
    public long selected;
    public long underReview;
    public long waitlisted;
    public long rejected;
    public long applied;
    public List<JobStat> jobStats;

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class JobStat {
        public String jobTitle;
        public long applicants;
        public double avgScore;
    }
}

// Export wrapper
@Data @AllArgsConstructor @NoArgsConstructor
class ExportRow {
    public String candidateName;
    public String email;
    public String jobTitle;
    public double matchScore;
    public double atsScore;
    public String status;
    public String skillGap;
}
