package com.resumeiq.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Uses Claude AI to perform deep resume analysis:
 *  - Extract structured candidate profile
 *  - Score and rank against a job description
 *  - Generate personalised skill gap report
 *  - Write tailored improvement suggestions
 *  - Recommend keyword additions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIResumeAnalysisService {

    private final ClaudeAIService claude;

    private static final String SYSTEM_PROFILE =
        "You are an expert recruitment AI. Your job is to read resume text and extract " +
        "a clean, structured candidate profile. Always respond with valid JSON only — " +
        "no explanation, no markdown fences, just the raw JSON object.";

    private static final String SYSTEM_SCORING =
        "You are a senior technical recruiter and AI talent evaluator. " +
        "You deeply understand job requirements and candidate fit. " +
        "Always respond with valid JSON only — no explanation, no markdown.";

    private static final String SYSTEM_SUGGESTIONS =
        "You are a professional resume coach and career advisor with 15 years of experience. " +
        "Give specific, actionable, personalised advice. " +
        "Always respond with valid JSON only — no markdown fences.";

    // ── 1. Extract candidate profile from raw resume text ─────────────────
    public Map<String, Object> extractProfile(String resumeText) {
        String prompt = """
            Extract a structured profile from this resume text.
            
            Resume text:
            ---
            %s
            ---
            
            Return ONLY this JSON structure:
            {
              "fullName": "candidate full name or Unknown",
              "email": "email or empty string",
              "phone": "phone number or empty string",
              "totalExperienceYears": <integer estimate>,
              "skills": ["skill1", "skill2", ...],
              "topSkills": ["top 5 most prominent skills"],
              "education": [
                {"degree": "...", "institution": "...", "year": "..."}
              ],
              "workHistory": [
                {"company": "...", "title": "...", "duration": "...", "highlights": ["..."]}
              ],
              "summary": "2-3 sentence professional summary of the candidate",
              "certifications": ["cert1", "cert2"],
              "languages": ["English", ...],
              "strengths": ["3-5 key strengths observed in resume"]
            }
            """.formatted(truncate(resumeText, 6000));

        JsonNode result = claude.askJson(SYSTEM_PROFILE, prompt);
        if (result == null) return fallbackProfile(resumeText);

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("fullName",             textOrDefault(result, "fullName", "Unknown Candidate"));
        profile.put("email",                textOrDefault(result, "email", ""));
        profile.put("phone",                textOrDefault(result, "phone", ""));
        profile.put("totalExperienceYears", result.path("totalExperienceYears").asInt(0));
        profile.put("skills",               jsonArrayToList(result.path("skills")));
        profile.put("topSkills",            jsonArrayToList(result.path("topSkills")));
        profile.put("education",            result.path("education").toString());
        profile.put("workHistory",          result.path("workHistory").toString());
        profile.put("summary",              textOrDefault(result, "summary", ""));
        profile.put("certifications",       jsonArrayToList(result.path("certifications")));
        profile.put("languages",            jsonArrayToList(result.path("languages")));
        profile.put("strengths",            jsonArrayToList(result.path("strengths")));
        return profile;
    }

    // ── 2. Score resume against a job description ──────────────────────────
    public Map<String, Object> scoreAgainstJob(String resumeText, String jobTitle,
                                                String jobDescription, String requiredSkills,
                                                int requiredExperience) {
        String prompt = """
            You are evaluating a candidate's resume against a specific job role.
            
            JOB ROLE: %s
            JOB DESCRIPTION: %s
            REQUIRED SKILLS: %s
            MINIMUM EXPERIENCE: %d years
            
            RESUME TEXT:
            ---
            %s
            ---
            
            Perform a deep evaluation and return ONLY this JSON:
            {
              "matchScore": <0-100 float, overall fit score>,
              "atsScore": <0-100 float, ATS keyword compatibility>,
              "skillMatchScore": <0-100 float, skill overlap score>,
              "experienceScore": <0-100 float, experience fit score>,
              "educationScore": <0-100 float, education fit score>,
              "matchedSkills": ["skills candidate has that job needs"],
              "missingSkills": ["required skills candidate lacks"],
              "bonusSkills": ["candidate skills beyond requirements"],
              "experienceFit": "brief assessment of experience fit",
              "overallVerdict": "STRONG_MATCH | GOOD_MATCH | PARTIAL_MATCH | WEAK_MATCH",
              "verdictReason": "1-2 sentence explanation of the verdict",
              "hiringRecommendation": "RECOMMEND | CONSIDER | SKIP"
            }
            """.formatted(jobTitle, jobDescription, requiredSkills,
                         requiredExperience, truncate(resumeText, 5000));

        JsonNode result = claude.askJson(SYSTEM_SCORING, prompt);
        if (result == null) return fallbackScore();

        Map<String, Object> score = new LinkedHashMap<>();
        score.put("matchScore",           result.path("matchScore").asDouble(40));
        score.put("atsScore",             result.path("atsScore").asDouble(40));
        score.put("skillMatchScore",      result.path("skillMatchScore").asDouble(40));
        score.put("experienceScore",      result.path("experienceScore").asDouble(40));
        score.put("educationScore",       result.path("educationScore").asDouble(40));
        score.put("matchedSkills",        jsonArrayToList(result.path("matchedSkills")));
        score.put("missingSkills",        jsonArrayToList(result.path("missingSkills")));
        score.put("bonusSkills",          jsonArrayToList(result.path("bonusSkills")));
        score.put("experienceFit",        textOrDefault(result, "experienceFit", ""));
        score.put("overallVerdict",       textOrDefault(result, "overallVerdict", "PARTIAL_MATCH"));
        score.put("verdictReason",        textOrDefault(result, "verdictReason", ""));
        score.put("hiringRecommendation", textOrDefault(result, "hiringRecommendation", "CONSIDER"));
        return score;
    }

    // ── 3. Generate personalised improvement suggestions ───────────────────
    public Map<String, Object> generateSuggestions(String resumeText, String jobTitle,
                                                     String missingSkills, String jobDescription) {
        String prompt = """
            A candidate has applied for the role of "%s".
            
            Their resume (summarised):
            ---
            %s
            ---
            
            Missing skills identified: %s
            Job description: %s
            
            As a professional resume coach, provide specific, actionable advice.
            Return ONLY this JSON:
            {
              "resumeImprovements": [
                "specific improvement 1",
                "specific improvement 2",
                "specific improvement 3",
                "specific improvement 4",
                "specific improvement 5"
              ],
              "skillsToLearn": [
                {"skill": "skill name", "reason": "why this skill matters for the role", "priority": "HIGH|MEDIUM|LOW"}
              ],
              "keywordsToAdd": ["keyword1", "keyword2", ...],
              "summaryRewrite": "Rewrite their professional summary optimised for this role in 2-3 sentences",
              "interviewTips": [
                "tip 1 for interviewing for this role",
                "tip 2",
                "tip 3"
              ],
              "overallAdvice": "2-3 sentences of overall strategic career advice"
            }
            """.formatted(jobTitle, truncate(resumeText, 3000), missingSkills, truncate(jobDescription, 500));

        JsonNode result = claude.askJson(SYSTEM_SUGGESTIONS, prompt);
        if (result == null) return fallbackSuggestions();

        Map<String, Object> suggestions = new LinkedHashMap<>();
        suggestions.put("resumeImprovements", jsonArrayToList(result.path("resumeImprovements")));
        suggestions.put("skillsToLearn",      result.path("skillsToLearn").toString());
        suggestions.put("keywordsToAdd",      jsonArrayToList(result.path("keywordsToAdd")));
        suggestions.put("summaryRewrite",     textOrDefault(result, "summaryRewrite", ""));
        suggestions.put("interviewTips",      jsonArrayToList(result.path("interviewTips")));
        suggestions.put("overallAdvice",      textOrDefault(result, "overallAdvice", ""));
        return suggestions;
    }

    // ── 4. Cross-role alternative job matching ─────────────────────────────
    public List<Map<String, Object>> findAlternativeRoles(String candidateSummary,
                                                           List<Map<String, String>> openJobs) {
        if (openJobs.isEmpty()) return List.of();

        String jobList = openJobs.stream()
                .map(j -> "- ID:%s | Title:%s | Skills:%s".formatted(j.get("id"), j.get("title"), j.get("skills")))
                .reduce("", (a, b) -> a + "\n" + b);

        String prompt = """
            A candidate has this profile:
            %s
            
            Available open job roles:
            %s
            
            For each job, evaluate how well the candidate fits.
            Return ONLY a JSON array:
            [
              {
                "jobId": "id from list",
                "jobTitle": "title",
                "fitScore": <0-100>,
                "fitReason": "1 sentence why they fit",
                "recommendation": "STRONG_FIT | GOOD_FIT | POSSIBLE_FIT"
              }
            ]
            Sort by fitScore descending. Include only jobs with fitScore >= 30.
            """.formatted(candidateSummary, jobList);

        JsonNode result = claude.askJson(SYSTEM_SCORING, prompt);
        List<Map<String, Object>> roles = new ArrayList<>();

        if (result != null && result.isArray()) {
            for (JsonNode node : result) {
                Map<String, Object> role = new LinkedHashMap<>();
                role.put("jobId",          textOrDefault(node, "jobId", ""));
                role.put("jobTitle",       textOrDefault(node, "jobTitle", ""));
                role.put("fitScore",       node.path("fitScore").asDouble(0));
                role.put("fitReason",      textOrDefault(node, "fitReason", ""));
                role.put("recommendation", textOrDefault(node, "recommendation", "POSSIBLE_FIT"));
                roles.add(role);
            }
        }
        return roles;
    }

    // ── 5. Parse and understand a job description ──────────────────────────
    public Map<String, Object> parseJobDescription(String jobDescriptionText) {
        String prompt = """
            Parse this job description and extract structured requirements.
            
            Job Description:
            ---
            %s
            ---
            
            Return ONLY this JSON:
            {
              "coreSkills": ["must-have skill 1", ...],
              "niceToHaveSkills": ["optional skill 1", ...],
              "experienceLevel": "Junior|Mid|Senior|Lead|Principal",
              "minExperienceYears": <integer>,
              "educationRequired": "degree requirement or None",
              "responsibilities": ["responsibility 1", ...],
              "perksAndBenefits": ["benefit 1", ...],
              "companyCulture": "brief culture description if mentioned"
            }
            """.formatted(truncate(jobDescriptionText, 3000));

        JsonNode result = claude.askJson(SYSTEM_PROFILE, prompt);
        if (result == null) return Map.of();

        Map<String, Object> parsed = new LinkedHashMap<>();
        parsed.put("coreSkills",        jsonArrayToList(result.path("coreSkills")));
        parsed.put("niceToHaveSkills",  jsonArrayToList(result.path("niceToHaveSkills")));
        parsed.put("experienceLevel",   textOrDefault(result, "experienceLevel", "Mid"));
        parsed.put("minExperienceYears",result.path("minExperienceYears").asInt(0));
        parsed.put("educationRequired", textOrDefault(result, "educationRequired", "Not specified"));
        parsed.put("responsibilities",  jsonArrayToList(result.path("responsibilities")));
        parsed.put("perksAndBenefits",  jsonArrayToList(result.path("perksAndBenefits")));
        parsed.put("companyCulture",    textOrDefault(result, "companyCulture", ""));
        return parsed;
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private String truncate(String text, int maxChars) {
        if (text == null) return "";
        return text.length() > maxChars ? text.substring(0, maxChars) + "..." : text;
    }

    private String textOrDefault(JsonNode node, String field, String def) {
        JsonNode f = node.path(field);
        return f.isMissingNode() || f.isNull() ? def : f.asText(def);
    }

    private List<String> jsonArrayToList(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node.isArray()) {
            node.forEach(n -> {
                if (n.isTextual()) list.add(n.asText());
                else if (!n.isNull()) list.add(n.toString());
            });
        }
        return list;
    }

    // Fallbacks when Claude API is unavailable
    private Map<String, Object> fallbackProfile(String text) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("fullName", "Unknown Candidate");
        p.put("email", ""); p.put("phone", "");
        p.put("totalExperienceYears", 0);
        p.put("skills", List.of());
        p.put("topSkills", List.of());
        p.put("education", "[]");
        p.put("workHistory", "[]");
        p.put("summary", "Profile could not be extracted.");
        p.put("certifications", List.of());
        p.put("languages", List.of());
        p.put("strengths", List.of());
        return p;
    }

    private Map<String, Object> fallbackScore() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("matchScore", 0.0); s.put("atsScore", 0.0);
        s.put("skillMatchScore", 0.0); s.put("experienceScore", 0.0); s.put("educationScore", 0.0);
        s.put("matchedSkills", List.of()); s.put("missingSkills", List.of()); s.put("bonusSkills", List.of());
        s.put("experienceFit", "Unable to evaluate"); s.put("overallVerdict", "PARTIAL_MATCH");
        s.put("verdictReason", "AI scoring unavailable"); s.put("hiringRecommendation", "CONSIDER");
        return s;
    }

    private Map<String, Object> fallbackSuggestions() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("resumeImprovements", List.of("Improve resume formatting", "Add more keywords"));
        s.put("skillsToLearn", "[]");
        s.put("keywordsToAdd", List.of());
        s.put("summaryRewrite", "");
        s.put("interviewTips", List.of());
        s.put("overallAdvice", "Focus on aligning your skills with the job requirements.");
        return s;
    }

    // ── 6. Compare two candidates side-by-side ──────────────────────────────
    public Map<String, Object> compareCandidates(String r1Text, String r1Name,
                                                 String r2Text, String r2Name,
                                                 String jobTitle, String jobDescription) {
        String jobCtx = (jobTitle != null && !jobTitle.isBlank())
            ? "JOB TITLE: %s\nJOB DESCRIPTION: %s\n".formatted(jobTitle, jobDescription)
            : "No specific job role context. General comparison.";

        String prompt = """
            You are a senior technical recruiter and talent advisor.
            Compare these two candidates side-by-side.

            %s

            CANDIDATE 1: %s
            RESUME TEXT 1:
            ---
            %s
            ---

            CANDIDATE 2: %s
            RESUME TEXT 2:
            ---
            %s
            ---

            Perform a side-by-side comparison. Respond with valid JSON ONLY matching this structure. Do NOT include markdown fences (like ```json), explanation, or other text:
            {
              "candidate1Summary": "1-2 sentence professional summary of Candidate 1",
              "candidate2Summary": "1-2 sentence professional summary of Candidate 2",
              "comparisonGrid": [
                {"criteria": "Experience Fit", "candidate1": "Brief analysis for Cand 1", "candidate2": "Brief analysis for Cand 2", "winner": "CANDIDATE_1|CANDIDATE_2|TIE"},
                {"criteria": "Skills Alignment", "candidate1": "...", "candidate2": "...", "winner": "CANDIDATE_1|CANDIDATE_2|TIE"},
                {"criteria": "Education Fit", "candidate1": "...", "candidate2": "...", "winner": "CANDIDATE_1|CANDIDATE_2|TIE"},
                {"criteria": "Key Strengths", "candidate1": "...", "candidate2": "...", "winner": "CANDIDATE_1|CANDIDATE_2|TIE"}
              ],
              "candidate1Pros": ["pro 1", "pro 2"],
              "candidate2Pros": ["pro 1", "pro 2"],
              "candidate1Cons": ["con 1", "con 2"],
              "candidate2Cons": ["con 1", "con 2"],
              "recommendation": "Detailed recommendation on who is the better fit for the role and why"
            }
            """.formatted(jobCtx, r1Name, truncate(r1Text, 4000), r2Name, truncate(r2Text, 4000));

        JsonNode result = claude.askJson(SYSTEM_SCORING, prompt);
        if (result == null) return fallbackComparison(r1Name, r2Name);

        Map<String, Object> comparison = new LinkedHashMap<>();
        comparison.put("candidate1Summary", textOrDefault(result, "candidate1Summary", ""));
        comparison.put("candidate2Summary", textOrDefault(result, "candidate2Summary", ""));

        List<Map<String, String>> grid = new ArrayList<>();
        JsonNode gridNode = result.path("comparisonGrid");
        if (gridNode.isArray()) {
            for (JsonNode node : gridNode) {
                Map<String, String> row = new LinkedHashMap<>();
                row.put("criteria", textOrDefault(node, "criteria", ""));
                row.put("candidate1", textOrDefault(node, "candidate1", ""));
                row.put("candidate2", textOrDefault(node, "candidate2", ""));
                row.put("winner", textOrDefault(node, "winner", "TIE"));
                grid.add(row);
            }
        }
        comparison.put("comparisonGrid", grid);
        comparison.put("candidate1Pros",    jsonArrayToList(result.path("candidate1Pros")));
        comparison.put("candidate2Pros",    jsonArrayToList(result.path("candidate2Pros")));
        comparison.put("candidate1Cons",    jsonArrayToList(result.path("candidate1Cons")));
        comparison.put("candidate2Cons",    jsonArrayToList(result.path("candidate2Cons")));
        comparison.put("recommendation",    textOrDefault(result, "recommendation", ""));
        return comparison;
    }

    private Map<String, Object> fallbackComparison(String name1, String name2) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("candidate1Summary", "Comparison unavailable");
        c.put("candidate2Summary", "Comparison unavailable");
        c.put("comparisonGrid", List.of());
        c.put("candidate1Pros", List.of());
        c.put("candidate2Pros", List.of());
        c.put("candidate1Cons", List.of());
        c.put("candidate2Cons", List.of());
        c.put("recommendation", "Unable to compare candidates at this time.");
        return c;
    }
}
