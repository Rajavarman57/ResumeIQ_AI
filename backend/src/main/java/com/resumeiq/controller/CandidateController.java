package com.resumeiq.controller;

import com.resumeiq.model.*;
import com.resumeiq.repository.*;
import com.resumeiq.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/candidates")
@RequiredArgsConstructor
public class CandidateController {

    private final CandidateScoreRepository scoreRepo;
    private final CandidateStatusRepository statusRepo;
    private final ResumeRepository resumeRepo;
    private final UserRepository userRepo;
    private final MatchingService matcher;
    private final EmailService emailService;
    private final AIResumeAnalysisService aiService;
    private final JobRoleRepository jobRepo;

    @GetMapping("/ranked/{jobId}")
    public ResponseEntity<?> ranked(@PathVariable Long jobId,
                                    @RequestParam(required = false) String skills) {
        List<CandidateScore> scores = scoreRepo.findByJobRoleIdOrderByMatchScoreDesc(jobId);
        if (skills != null && !skills.isBlank()) {
            List<String> filter = Arrays.stream(skills.split(","))
                    .map(String::trim).map(String::toLowerCase).collect(Collectors.toList());
            scores = scores.stream().filter(s -> {
                String rs = s.getResume().getExtractedSkills();
                return rs != null && filter.stream().anyMatch(rs.toLowerCase()::contains);
            }).collect(Collectors.toList());
        }
        return ResponseEntity.ok(scores.stream().map(this::toMap).collect(Collectors.toList()));
    }

    @GetMapping("/resume/{resumeId}/scores")
    public ResponseEntity<?> resumeScores(@PathVariable Long resumeId) {
        return ResponseEntity.ok(scoreRepo.findByResumeId(resumeId)
                .stream().map(this::toMap).collect(Collectors.toList()));
    }

    @PutMapping("/status/{resumeId}/{jobId}")
    public ResponseEntity<?> updateStatus(@PathVariable Long resumeId,
                                           @PathVariable Long jobId,
                                           @RequestBody Map<String,String> req,
                                           org.springframework.security.core.Authentication auth) {
        User user = userRepo.findByEmail(auth.getName()).orElseThrow();
        CandidateStatus status = statusRepo.findByResumeIdAndJobRoleId(resumeId, jobId)
                .orElse(new CandidateStatus());
        Resume resume = resumeRepo.findById(resumeId).orElseThrow();
        try {
            CandidateScore score = scoreRepo.findByResumeIdAndJobRoleId(resumeId, jobId).orElseThrow();
            status.setResume(resume);
            status.setJobRole(score.getJobRole());
            status.setStatus(CandidateStatus.Status.valueOf(req.get("status")));
            status.setNotes(req.get("notes"));
            status.setUpdatedBy(user);
            statusRepo.save(status);

            // Send email notification async
            String candidateEmail = resume.getCandidateEmail();
            if (candidateEmail != null && !candidateEmail.isBlank()) {
                List<String> tips = parseTips(score.getSuggestions());
                emailService.sendStatusUpdate(
                        candidateEmail,
                        resume.getCandidateName(),
                        score.getJobRole().getTitle(),
                        req.get("status"),
                        score.getMatchScore(),
                        score.getAtsScore(),
                        req.get("notes"),
                        tips.subList(0, Math.min(3, tips.size()))
                );
            }
            return ResponseEntity.ok(Map.of("message","Status updated","emailSent", candidateEmail != null && !candidateEmail.isBlank()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error","Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/status/{resumeId}/{jobId}")
    public ResponseEntity<?> getStatus(@PathVariable Long resumeId, @PathVariable Long jobId) {
        return statusRepo.findByResumeIdAndJobRoleId(resumeId, jobId)
                .map(s -> ResponseEntity.ok(Map.of(
                        "status", s.getStatus().name(),
                        "notes",  s.getNotes() != null ? s.getNotes() : "",
                        "updatedAt", s.getUpdatedAt().toString())))
                .orElse(ResponseEntity.ok(Map.of("status","APPLIED","notes","")));
    }

    @GetMapping("/alternatives/{resumeId}/{jobId}")
    public ResponseEntity<?> alternatives(@PathVariable Long resumeId, @PathVariable Long jobId) {
        Resume resume = resumeRepo.findById(resumeId).orElseThrow();
        List<Map<String,Object>> aiAlts = matcher.getAlternativeJobsAI(resume, jobId);
        List<String> simple = matcher.getAlternativeJobs(resume, jobId);
        return ResponseEntity.ok(Map.of("alternatives",simple,"aiAlternatives",aiAlts));
    }

    @GetMapping("/score/{resumeId}/{jobId}")
    public ResponseEntity<?> scoreDetail(@PathVariable Long resumeId, @PathVariable Long jobId) {
        return scoreRepo.findByResumeIdAndJobRoleId(resumeId, jobId)
                .map(s -> ResponseEntity.ok(toMap(s)))
                .orElse(ResponseEntity.notFound().build());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private List<String> parseTips(String suggestions) {
        if (suggestions == null) return List.of();
        String improve = extractSection(suggestions, "IMPROVE:");
        if (improve.isBlank()) return List.of();
        return Arrays.stream(improve.split("•")).map(String::trim).filter(s -> !s.isBlank()).collect(Collectors.toList());
    }

    private Map<String,Object> toMap(CandidateScore s) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("scoreId",       s.getId());
        m.put("resumeId",      s.getResume().getId());
        m.put("candidateName", s.getResume().getCandidateName());
        m.put("candidateEmail",s.getResume().getCandidateEmail());
        m.put("candidatePhone",s.getResume().getCandidatePhone());
        m.put("extractedSkills",s.getResume().getExtractedSkills());
        m.put("experienceYears",s.getResume().getExperienceYears());
        m.put("education",     s.getResume().getEducation());
        m.put("jobId",         s.getJobRole().getId());
        m.put("jobTitle",      s.getJobRole().getTitle());
        m.put("matchScore",    s.getMatchScore());
        m.put("atsScore",      s.getAtsScore());

        String kw  = s.getKeywords()   != null ? s.getKeywords()   : "";
        String sug = s.getSuggestions()!= null ? s.getSuggestions(): "";

        m.put("matchedSkills", extractList(kw,  "MATCHED:"));
        m.put("bonusSkills",   extractList(kw,  "BONUS:"));
        m.put("keywordsToAdd", extractList(kw,  "ADD:"));
        m.put("verdict",       extractSection(sug,"VERDICT:"));
        m.put("recommendation",extractSection(sug,"REC:"));
        m.put("suggestions",   sug.replaceAll("VERDICT:[^|]+\\|?","").replaceAll("REC:[^|]+\\|?","").trim());
        m.put("skillGap",      s.getSkillGap());

        statusRepo.findByResumeIdAndJobRoleId(s.getResume().getId(), s.getJobRole().getId())
                .ifPresent(st -> { m.put("status", st.getStatus().name()); m.put("notes", st.getNotes()!=null?st.getNotes():""); });
        if (!m.containsKey("status")) m.put("status","APPLIED");
        return m;
    }

    @GetMapping("/compare/{id1}/{id2}")
    public ResponseEntity<?> compare(@PathVariable Long id1,
                                     @PathVariable Long id2,
                                     @RequestParam(required = false) Long jobId) {
        Resume r1 = resumeRepo.findById(id1)
                .orElseThrow(() -> new RuntimeException("Candidate 1 not found"));
        Resume r2 = resumeRepo.findById(id2)
                .orElseThrow(() -> new RuntimeException("Candidate 2 not found"));

        JobRole job = null;
        if (jobId != null) {
            job = jobRepo.findById(jobId).orElse(null);
        }

        Map<String, Object> comparison = aiService.compareCandidates(
                r1.getRawText(), r1.getCandidateName(),
                r2.getRawText(), r2.getCandidateName(),
                job != null ? job.getTitle() : null,
                job != null ? job.getDescription() : null
        );

        return ResponseEntity.ok(comparison);
    }

    private List<String> extractList(String text, String prefix) {
        String sec = extractSection(text, prefix);
        if (sec.isBlank()) return List.of();
        return Arrays.stream(sec.split(",")).map(String::trim).filter(s->!s.isBlank()).collect(Collectors.toList());
    }

    private String extractSection(String text, String prefix) {
        if (text == null || !text.contains(prefix)) return "";
        int start = text.indexOf(prefix) + prefix.length();
        int end   = text.indexOf(" | ", start);
        return (end > start ? text.substring(start,end) : text.substring(start)).trim();
    }
}
