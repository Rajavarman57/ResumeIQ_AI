package com.resumeiq.service;

import com.resumeiq.model.*;
import com.resumeiq.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingService {

    private final CandidateScoreRepository scoreRepo;
    private final JobRoleRepository jobRepo;
    private final AIResumeAnalysisService aiService;

    /**
     * AI-powered scoring of a resume against a job role.
     * Claude evaluates semantic fit, not just keyword overlap.
     */
    public CandidateScore scoreResumeAgainstJob(Resume resume, JobRole job) {

        log.info("AI scoring: {} against {}", resume.getCandidateName(), job.getTitle());

        // Ask Claude to score this resume against the job
        Map<String, Object> aiScore = aiService.scoreAgainstJob(
                resume.getRawText(),
                job.getTitle(),
                job.getDescription() != null ? job.getDescription() : "",
                job.getRequiredSkills(),
                job.getExperienceYears()
        );

        // Extract AI results
        double matchScore   = toDouble(aiScore.get("matchScore"), 40.0);
        double atsScore     = toDouble(aiScore.get("atsScore"), 40.0);
        String verdict      = str(aiScore.get("overallVerdict"), "PARTIAL_MATCH");
        String verdictReason= str(aiScore.get("verdictReason"), "");
        String hiringRec    = str(aiScore.get("hiringRecommendation"), "CONSIDER");
        String experienceFit= str(aiScore.get("experienceFit"), "");

        @SuppressWarnings("unchecked")
        List<String> missingSkills = (List<String>) aiScore.getOrDefault("missingSkills", List.of());
        @SuppressWarnings("unchecked")
        List<String> matchedSkills = (List<String>) aiScore.getOrDefault("matchedSkills", List.of());
        @SuppressWarnings("unchecked")
        List<String> bonusSkills   = (List<String>) aiScore.getOrDefault("bonusSkills", List.of());

        // Generate AI suggestions
        Map<String, Object> aiSuggestions = aiService.generateSuggestions(
                resume.getRawText(),
                job.getTitle(),
                String.join(", ", missingSkills),
                job.getDescription() != null ? job.getDescription() : ""
        );

        @SuppressWarnings("unchecked")
        List<String> improvements = (List<String>) aiSuggestions.getOrDefault("resumeImprovements", List.of());
        @SuppressWarnings("unchecked")
        List<String> keywords     = (List<String>) aiSuggestions.getOrDefault("keywordsToAdd", List.of());
        @SuppressWarnings("unchecked")
        List<String> interviewTips= (List<String>) aiSuggestions.getOrDefault("interviewTips", List.of());
        String summaryRewrite     = str(aiSuggestions.get("summaryRewrite"), "");
        String overallAdvice      = str(aiSuggestions.get("overallAdvice"), "");

        // Build rich suggestion text
        String suggestionText = buildSuggestionText(improvements, interviewTips, summaryRewrite, overallAdvice, experienceFit, verdictReason, hiringRec);
        String skillGapText   = String.join(", ", missingSkills);
        String keywordText    = String.join(", ", keywords);

        // Upsert
        Optional<CandidateScore> existing = scoreRepo.findByResumeIdAndJobRoleId(resume.getId(), job.getId());
        CandidateScore score = existing.orElse(CandidateScore.builder()
                .resume(resume).jobRole(job).build());

        score.setMatchScore(round(matchScore));
        score.setAtsScore(round(atsScore));
        score.setSkillGap(skillGapText);
        score.setSuggestions(suggestionText);
        score.setKeywords(keywordText);

        // Store extra AI fields as JSON in existing columns (reusing keywords/suggestions for rich data)
        // Store matched skills in keywords field prefix
        String richKeywords = "MATCHED:" + String.join(",", matchedSkills)
                + " | BONUS:" + String.join(",", bonusSkills)
                + " | ADD:" + keywordText;
        score.setKeywords(richKeywords);

        // Store verdict in suggestions
        String richSuggestions = "VERDICT:" + verdict + " | REC:" + hiringRec + " | " + suggestionText;
        score.setSuggestions(richSuggestions);

        return scoreRepo.save(score);
    }

    /**
     * AI-powered cross-role alternative job matching.
     */
    public List<Map<String, Object>> getAlternativeJobsAI(Resume resume, Long excludeJobId) {
        List<JobRole> openJobs = jobRepo.findByStatus(JobRole.Status.OPEN).stream()
                .filter(j -> !j.getId().equals(excludeJobId))
                .collect(Collectors.toList());

        if (openJobs.isEmpty()) return List.of();

        List<Map<String, String>> jobList = openJobs.stream().map(j -> Map.of(
                "id",     j.getId().toString(),
                "title",  j.getTitle(),
                "skills", j.getRequiredSkills() != null ? j.getRequiredSkills() : ""
        )).collect(Collectors.toList());

        String candidateSummary = buildCandidateSummary(resume);
        return aiService.findAlternativeRoles(candidateSummary, jobList);
    }

    // Legacy string-based alternative (fallback)
    public List<String> getAlternativeJobs(Resume resume, Long currentJobId) {
        List<Map<String, Object>> aiAlts = getAlternativeJobsAI(resume, currentJobId);
        return aiAlts.stream()
                .map(a -> a.get("jobTitle") + " (" + Math.round((double) a.get("fitScore")) + "% fit — " + a.get("fitReason") + ")")
                .collect(Collectors.toList());
    }

    private String buildCandidateSummary(Resume resume) {
        return String.format(
            "Name: %s | Skills: %s | Experience: %d years | Education: %s",
            resume.getCandidateName(),
            resume.getExtractedSkills() != null ? resume.getExtractedSkills() : "Unknown",
            resume.getExperienceYears(),
            resume.getEducation() != null ? resume.getEducation() : "Unknown"
        );
    }

    private String buildSuggestionText(List<String> improvements, List<String> tips,
                                        String rewrite, String advice, String expFit,
                                        String verdictReason, String rec) {
        StringBuilder sb = new StringBuilder();
        if (!verdictReason.isBlank()) sb.append("ASSESSMENT: ").append(verdictReason).append(" | ");
        if (!expFit.isBlank()) sb.append("EXPERIENCE: ").append(expFit).append(" | ");
        if (!improvements.isEmpty()) sb.append("IMPROVE: ").append(String.join(" • ", improvements)).append(" | ");
        if (!tips.isEmpty()) sb.append("INTERVIEW: ").append(String.join(" • ", tips)).append(" | ");
        if (!rewrite.isBlank()) sb.append("SUMMARY: ").append(rewrite).append(" | ");
        if (!advice.isBlank()) sb.append("ADVICE: ").append(advice);
        return sb.toString();
    }

    private double toDouble(Object val, double def) {
        if (val == null) return def;
        try { return ((Number) val).doubleValue(); } catch (Exception e) { return def; }
    }

    private String str(Object val, String def) {
        return val == null ? def : val.toString();
    }

    private double round(double v) { return Math.round(v * 10.0) / 10.0; }
}
