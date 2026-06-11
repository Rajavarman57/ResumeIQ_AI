package com.resumeiq.controller;

import com.resumeiq.model.*;
import com.resumeiq.repository.*;
import com.resumeiq.service.AIResumeAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobRoleRepository jobRepo;
    private final UserRepository userRepo;
    private final AIResumeAnalysisService aiService;

    @GetMapping
    public ResponseEntity<?> all() {
        return ResponseEntity.ok(jobRepo.findAll().stream().map(this::toMap).collect(Collectors.toList()));
    }

    @GetMapping("/open")
    public ResponseEntity<?> open() {
        return ResponseEntity.ok(jobRepo.findByStatus(JobRole.Status.OPEN)
                .stream().map(this::toMap).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return jobRepo.findById(id).map(j -> ResponseEntity.ok(toMap(j)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> req,
                                    org.springframework.security.core.Authentication auth) {
        User user = userRepo.findByEmail(auth.getName()).orElseThrow();
        JobRole job = JobRole.builder()
                .title((String) req.get("title"))
                .description((String) req.get("description"))
                .requiredSkills((String) req.get("requiredSkills"))
                .experienceYears(req.get("experienceYears") != null ?
                        ((Number) req.get("experienceYears")).intValue() : 0)
                .status(JobRole.Status.OPEN)
                .createdBy(user)
                .build();
        return ResponseEntity.ok(toMap(jobRepo.save(job)));
    }

    // AI-powered: parse a free-text job description and auto-extract skills
    @PostMapping("/ai-parse")
    public ResponseEntity<?> aiParse(@RequestBody Map<String, String> req) {
        String description = req.get("description");
        if (description == null || description.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "description required"));
        Map<String, Object> parsed = aiService.parseJobDescription(description);
        return ResponseEntity.ok(parsed);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> req) {
        return jobRepo.findById(id).map(job -> {
            if (req.containsKey("title")) job.setTitle((String) req.get("title"));
            if (req.containsKey("description")) job.setDescription((String) req.get("description"));
            if (req.containsKey("requiredSkills")) job.setRequiredSkills((String) req.get("requiredSkills"));
            if (req.containsKey("experienceYears"))
                job.setExperienceYears(((Number) req.get("experienceYears")).intValue());
            if (req.containsKey("status")) {
                try { job.setStatus(JobRole.Status.valueOf((String) req.get("status"))); }
                catch (Exception ignored) {}
            }
            return ResponseEntity.ok(toMap(jobRepo.save(job)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return jobRepo.findById(id).map(job -> {
            job.setStatus(JobRole.Status.ARCHIVED);
            jobRepo.save(job);
            return ResponseEntity.ok(Map.of("message", "Job archived"));
        }).orElse(ResponseEntity.notFound().build());
    }

    private Map<String, Object> toMap(JobRole j) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", j.getId());
        m.put("title", j.getTitle());
        m.put("description", j.getDescription());
        m.put("requiredSkills", j.getRequiredSkills());
        m.put("experienceYears", j.getExperienceYears());
        m.put("status", j.getStatus().name());
        m.put("createdAt", j.getCreatedAt());
        m.put("createdBy", j.getCreatedBy() != null ? j.getCreatedBy().getFullName() : "");
        return m;
    }
}
