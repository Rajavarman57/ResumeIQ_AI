package com.resumeiq.controller;

import com.resumeiq.model.*;
import com.resumeiq.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final ResumeRepository resumeRepo;
    private final JobRoleRepository jobRepo;
    private final CandidateScoreRepository scoreRepo;
    private final CandidateStatusRepository statusRepo;
    private final UserRepository userRepo;

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard(org.springframework.security.core.Authentication auth) {
        User user = userRepo.findByEmail(auth.getName()).orElseThrow();
        Map<String,Object> data = new LinkedHashMap<>();
        
        boolean isRecruiter = user.getRole() == User.Role.RECRUITER;
        Long userId = user.getId();
        
        if (isRecruiter) {
            data.put("totalResumes", resumeRepo.countByUploadedById(userId));
            data.put("totalJobs", jobRepo.countByCreatedById(userId));
            data.put("openJobs", jobRepo.countByCreatedByIdAndStatus(userId, JobRole.Status.OPEN));
            data.put("selected",    statusRepo.countByStatusAndJobRoleCreatedById(CandidateStatus.Status.SELECTED, userId));
            data.put("underReview", statusRepo.countByStatusAndJobRoleCreatedById(CandidateStatus.Status.UNDER_REVIEW, userId));
            data.put("waitlisted",  statusRepo.countByStatusAndJobRoleCreatedById(CandidateStatus.Status.WAITLISTED, userId));
            data.put("rejected",    statusRepo.countByStatusAndJobRoleCreatedById(CandidateStatus.Status.REJECTED, userId));
            data.put("applied",     statusRepo.countByStatusAndJobRoleCreatedById(CandidateStatus.Status.APPLIED, userId));
            
            // Per-job stats for jobs created by the recruiter
            List<Map<String,Object>> jobStats = jobRepo.findByCreatedById(userId).stream().map(job -> {
                Map<String,Object> js = new LinkedHashMap<>();
                js.put("jobId", job.getId());
                js.put("jobTitle", job.getTitle());
                js.put("status", job.getStatus().name());
                js.put("applicants", statusRepo.countByJobRoleId(job.getId()));
                Double avg = scoreRepo.avgScoreByJob(job.getId());
                js.put("avgScore", avg != null ? Math.round(avg * 10.0) / 10.0 : 0);
                return js;
            }).collect(Collectors.toList());
            data.put("jobStats", jobStats);
            
            // Top candidates for recruiter's jobs
            List<Map<String,Object>> top = scoreRepo.findByJobRoleCreatedById(userId).stream()
                    .sorted(Comparator.comparingDouble(CandidateScore::getMatchScore).reversed())
                    .limit(5)
                    .map(s -> {
                        Map<String,Object> t = new LinkedHashMap<>();
                        t.put("candidateName", s.getResume().getCandidateName());
                        t.put("jobTitle", s.getJobRole().getTitle());
                        t.put("matchScore", s.getMatchScore());
                        return t;
                    })
                    .collect(Collectors.toList());
            data.put("topCandidates", top);
        } else {
            data.put("totalResumes", resumeRepo.count());
            data.put("totalJobs", jobRepo.count());
            data.put("openJobs", jobRepo.findByStatus(JobRole.Status.OPEN).size());
            data.put("selected",    statusRepo.countByStatus(CandidateStatus.Status.SELECTED));
            data.put("underReview", statusRepo.countByStatus(CandidateStatus.Status.UNDER_REVIEW));
            data.put("waitlisted",  statusRepo.countByStatus(CandidateStatus.Status.WAITLISTED));
            data.put("rejected",    statusRepo.countByStatus(CandidateStatus.Status.REJECTED));
            data.put("applied",     statusRepo.countByStatus(CandidateStatus.Status.APPLIED));
            
            // Per-job stats (All jobs)
            List<Map<String,Object>> jobStats = jobRepo.findAll().stream().map(job -> {
                Map<String,Object> js = new LinkedHashMap<>();
                js.put("jobId", job.getId());
                js.put("jobTitle", job.getTitle());
                js.put("status", job.getStatus().name());
                js.put("applicants", statusRepo.countByJobRoleId(job.getId()));
                Double avg = scoreRepo.avgScoreByJob(job.getId());
                js.put("avgScore", avg != null ? Math.round(avg * 10.0) / 10.0 : 0);
                return js;
            }).collect(Collectors.toList());
            data.put("jobStats", jobStats);
            
            // Top candidates across all jobs
            List<Map<String,Object>> top = scoreRepo.findAll().stream()
                    .sorted(Comparator.comparingDouble(CandidateScore::getMatchScore).reversed())
                    .limit(5)
                    .map(s -> {
                        Map<String,Object> t = new LinkedHashMap<>();
                        t.put("candidateName", s.getResume().getCandidateName());
                        t.put("jobTitle", s.getJobRole().getTitle());
                        t.put("matchScore", s.getMatchScore());
                        return t;
                    })
                    .collect(Collectors.toList());
            data.put("topCandidates", top);
        }
        
        return ResponseEntity.ok(data);
    }

    @GetMapping("/export/csv")
    public void exportCsv(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=resumeiq_report.csv");

        PrintWriter writer = response.getWriter();
        writer.println("Candidate Name,Email,Job Title,Match Score (%),ATS Score (%),Status,Skill Gap,Suggestions");

        scoreRepo.findAll().forEach(score -> {
            String status = statusRepo
                    .findByResumeIdAndJobRoleId(score.getResume().getId(), score.getJobRole().getId())
                    .map(s -> s.getStatus().name()).orElse("APPLIED");
            writer.printf("\"%s\",\"%s\",\"%s\",%.1f,%.1f,\"%s\",\"%s\",\"%s\"%n",
                    escape(score.getResume().getCandidateName()),
                    escape(score.getResume().getCandidateEmail()),
                    escape(score.getJobRole().getTitle()),
                    score.getMatchScore(),
                    score.getAtsScore(),
                    status,
                    escape(score.getSkillGap()),
                    escape(score.getSuggestions())
            );
        });
        writer.flush();
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\"", "\"\"");
    }
}
