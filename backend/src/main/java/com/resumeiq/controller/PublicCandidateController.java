package com.resumeiq.controller;

import com.resumeiq.model.*;
import com.resumeiq.repository.*;
import com.resumeiq.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
@Slf4j
public class PublicCandidateController {

    private final ResumeRepository resumeRepo;
    private final JobRoleRepository jobRepo;
    private final CandidateScoreRepository scoreRepo;
    private final CandidateStatusRepository statusRepo;
    private final ResumeParserService parser;
    private final MatchingService matcher;
    private final AIResumeAnalysisService aiService;
    private final EmailService emailService;

    @GetMapping("/jobs")
    public ResponseEntity<?> openJobs() {
        List<Map<String, Object>> openJobs = jobRepo.findByStatus(JobRole.Status.OPEN).stream()
                .map(j -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", j.getId());
                    m.put("title", j.getTitle());
                    m.put("description", j.getDescription());
                    m.put("requiredSkills", j.getRequiredSkills());
                    m.put("experienceYears", j.getExperienceYears());
                    return m;
                }).collect(Collectors.toList());
        return ResponseEntity.ok(openJobs);
    }

    @PostMapping("/resumes/upload")
    public ResponseEntity<?> publicUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("jobId") Long jobId) {
        try {
            String originalName = file.getOriginalFilename();
            String ext = originalName != null && originalName.contains(".")
                    ? originalName.substring(originalName.lastIndexOf(".") + 1).toUpperCase() : "PDF";

            if (!List.of("PDF", "DOCX").contains(ext))
                return ResponseEntity.badRequest().body(Map.of("error", "Only PDF and DOCX supported"));

            // Save file to disk
            String uploadDir = "./uploads";
            new File(uploadDir).mkdirs();
            String savedName = System.currentTimeMillis() + "_public_" + originalName;
            Path path = Paths.get(uploadDir, savedName);
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

            // Step 1: Extract raw text
            String rawText = parser.extractText(file, ext);

            // Step 2: AI Profile extraction
            Map<String, Object> profile = aiService.extractProfile(rawText);
            @SuppressWarnings("unchecked")
            List<String> skills = (List<String>) profile.getOrDefault("skills", List.of());

            // Step 3: Save Resume entity (uploadedBy is null for public candidate portal)
            Resume resume = Resume.builder()
                    .candidateName((String) profile.getOrDefault("fullName", "Unknown Candidate"))
                    .candidateEmail((String) profile.getOrDefault("email", ""))
                    .candidatePhone((String) profile.getOrDefault("phone", ""))
                    .fileName(originalName)
                    .fileType(Resume.FileType.valueOf(ext))
                    .filePath(path.toString())
                    .rawText(rawText)
                    .extractedSkills(String.join(", ", skills))
                    .experienceYears(((Number) profile.getOrDefault("totalExperienceYears", 0)).intValue())
                    .education((String) profile.getOrDefault("education", "[]"))
                    .uploadedBy(null)
                    .build();

            resume = resumeRepo.save(resume);

            // Step 4: Score against selected Job Role
            JobRole job = jobRepo.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("Job role not found"));

            matcher.scoreResumeAgainstJob(resume, job);

            // Save candidate status as APPLIED (updatedBy is null)
            CandidateStatus status = CandidateStatus.builder()
                    .resume(resume)
                    .jobRole(job)
                    .status(CandidateStatus.Status.APPLIED)
                    .updatedBy(null)
                    .build();
            statusRepo.save(status);

            // Step 5: Send confirmation email to candidate (if candidate email exists)
            String candidateEmail = resume.getCandidateEmail();
            if (candidateEmail != null && !candidateEmail.isBlank()) {
                try {
                    CandidateScore score = scoreRepo.findByResumeIdAndJobRoleId(resume.getId(), job.getId()).orElseThrow();
                    List<String> tips = parseTips(score.getSuggestions());
                    emailService.sendStatusUpdate(
                            candidateEmail,
                            resume.getCandidateName(),
                            job.getTitle(),
                            "APPLIED",
                            score.getMatchScore(),
                            score.getAtsScore(),
                            "Thank you for submitting your application. We have successfully received and processed your resume using AI evaluation.",
                            tips.subList(0, Math.min(3, tips.size()))
                    );
                } catch (Exception ex) {
                    log.warn("Could not send applicant confirmation email", ex);
                }
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Application submitted successfully",
                    "candidateName", resume.getCandidateName(),
                    "jobTitle", job.getTitle()
            ));

        } catch (Exception e) {
            log.error("Public resume submission failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Submission failed: " + e.getMessage()));
        }
    }

    private List<String> parseTips(String suggestions) {
        if (suggestions == null) return List.of();
        String improve = extractSection(suggestions, "IMPROVE:");
        if (improve.isBlank()) return List.of();
        return Arrays.stream(improve.split("•")).map(String::trim).filter(s -> !s.isBlank()).collect(Collectors.toList());
    }

    private String extractSection(String text, String prefix) {
        if (text == null || !text.contains(prefix)) return "";
        int start = text.indexOf(prefix) + prefix.length();
        int end   = text.indexOf(" | ", start);
        return (end > start ? text.substring(start,end) : text.substring(start)).trim();
    }
}
