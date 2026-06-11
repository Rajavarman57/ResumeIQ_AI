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
@RequestMapping("/resumes")
@RequiredArgsConstructor
@Slf4j
public class ResumeController {

    private final ResumeRepository resumeRepo;
    private final UserRepository userRepo;
    private final JobRoleRepository jobRepo;
    private final CandidateScoreRepository scoreRepo;
    private final CandidateStatusRepository statusRepo;
    private final ResumeParserService parser;
    private final MatchingService matcher;
    private final AIResumeAnalysisService aiService;

    @GetMapping
    public ResponseEntity<?> all() {
        return ResponseEntity.ok(resumeRepo.findAll().stream()
                .map(this::toMap).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return resumeRepo.findById(id).map(r -> ResponseEntity.ok(toMap(r)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "jobId", required = false) Long jobId,
            org.springframework.security.core.Authentication auth) {
        try {
            String originalName = file.getOriginalFilename();
            String ext = originalName != null && originalName.contains(".")
                    ? originalName.substring(originalName.lastIndexOf(".") + 1).toUpperCase() : "PDF";

            if (!List.of("PDF", "DOCX").contains(ext))
                return ResponseEntity.badRequest().body(Map.of("error", "Only PDF and DOCX supported"));

            // Save file to disk
            String uploadDir = "./uploads";
            new File(uploadDir).mkdirs();
            String savedName = System.currentTimeMillis() + "_" + originalName;
            Path path = Paths.get(uploadDir, savedName);
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

            // Step 1: Extract raw text using PDFBox / POI
            String rawText = parser.extractText(file, ext);
            log.info("Extracted {} chars of text from {}", rawText.length(), originalName);

            // Step 2: AI profile extraction via Claude
            Map<String, Object> profile = aiService.extractProfile(rawText);
            log.info("AI extracted profile for: {}", profile.get("fullName"));

            @SuppressWarnings("unchecked")
            List<String> skills = (List<String>) profile.getOrDefault("skills", List.of());

            User uploader = userRepo.findByEmail(auth.getName()).orElseThrow();

            // Step 3: Build Resume entity with AI-extracted data
            Resume resume = Resume.builder()
                    .candidateName((String) profile.getOrDefault("fullName", "Unknown"))
                    .candidateEmail((String) profile.getOrDefault("email", ""))
                    .candidatePhone((String) profile.getOrDefault("phone", ""))
                    .fileName(originalName)
                    .fileType(Resume.FileType.valueOf(ext))
                    .filePath(path.toString())
                    .rawText(rawText)
                    .extractedSkills(String.join(", ", skills))
                    .experienceYears(((Number) profile.getOrDefault("totalExperienceYears", 0)).intValue())
                    .education((String) profile.getOrDefault("education", "[]"))
                    .uploadedBy(uploader)
                    .build();

            resume = resumeRepo.save(resume);

            // Step 4: AI scoring against job(s)
            List<JobRole> jobs = jobId != null
                    ? jobRepo.findById(jobId).map(List::of).orElse(List.of())
                    : jobRepo.findByStatus(JobRole.Status.OPEN);

            for (JobRole job : jobs) {
                matcher.scoreResumeAgainstJob(resume, job);
                if (statusRepo.findByResumeIdAndJobRoleId(resume.getId(), job.getId()).isEmpty()) {
                    statusRepo.save(CandidateStatus.builder()
                            .resume(resume).jobRole(job)
                            .status(CandidateStatus.Status.APPLIED)
                            .updatedBy(uploader).build());
                }
            }

            // Return enriched response with AI profile
            Map<String, Object> result = toMap(resume);
            result.put("aiProfile", profile);
            result.put("jobsScored", jobs.size());
            result.put("message", "Resume parsed and AI-scored successfully");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Resume upload failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to process resume: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return resumeRepo.findById(id).map(r -> {
            scoreRepo.findByResumeId(r.getId()).forEach(scoreRepo::delete);
            statusRepo.findByResumeId(r.getId()).forEach(statusRepo::delete);
            resumeRepo.delete(r);
            return ResponseEntity.ok(Map.of("message", "Resume deleted"));
        }).orElse(ResponseEntity.notFound().build());
    }

    // Re-run AI analysis on existing resume
    @PostMapping("/{id}/reanalyze")
    public ResponseEntity<?> reanalyze(@PathVariable Long id) {
        return resumeRepo.findById(id).map(resume -> {
            Map<String, Object> profile = aiService.extractProfile(resume.getRawText());
            @SuppressWarnings("unchecked")
            List<String> skills = (List<String>) profile.getOrDefault("skills", List.of());
            resume.setExtractedSkills(String.join(", ", skills));
            resume.setExperienceYears(((Number) profile.getOrDefault("totalExperienceYears", 0)).intValue());
            resumeRepo.save(resume);
            return ResponseEntity.ok(Map.of("profile", profile, "message", "Re-analysis complete"));
        }).orElse(ResponseEntity.notFound().build());
    }

    private Map<String, Object> toMap(Resume r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("candidateName", r.getCandidateName());
        m.put("candidateEmail", r.getCandidateEmail());
        m.put("candidatePhone", r.getCandidatePhone());
        m.put("fileName", r.getFileName());
        m.put("fileType", r.getFileType().name());
        m.put("extractedSkills", r.getExtractedSkills());
        m.put("experienceYears", r.getExperienceYears());
        m.put("education", r.getEducation());
        m.put("uploadedAt", r.getUploadedAt());
        return m;
    }
}
