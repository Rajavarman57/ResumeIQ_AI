package com.resumeiq.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class AIResumeAnalysisServiceTest {

    private AIResumeAnalysisService service;

    @Mock
    private ClaudeAIService claude;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AIResumeAnalysisService(claude);
    }

    @Test
    public void testExtractProfile() throws Exception {
        String mockResponse = """
            {
              "fullName": "Jane Doe",
              "email": "jane@example.com",
              "phone": "123456",
              "totalExperienceYears": 5,
              "skills": ["Java", "Spring"],
              "topSkills": ["Java"],
              "education": [],
              "workHistory": [],
              "summary": "Experienced engineer",
              "certifications": [],
              "languages": ["English"],
              "strengths": ["Leadership"]
            }
            """;
        JsonNode jsonNode = mapper.readTree(mockResponse);
        when(claude.askJson(anyString(), anyString())).thenReturn(jsonNode);

        Map<String, Object> profile = service.extractProfile("Jane Doe resume text");

        assertEquals("Jane Doe", profile.get("fullName"));
        assertEquals("jane@example.com", profile.get("email"));
        assertEquals(5, profile.get("totalExperienceYears"));
        assertEquals("Experienced engineer", profile.get("summary"));
        
        List<?> skills = (List<?>) profile.get("skills");
        assertTrue(skills.contains("Java"));
    }

    @Test
    public void testScoreAgainstJob() throws Exception {
        String mockResponse = """
            {
              "matchScore": 85.0,
              "atsScore": 80.0,
              "skillMatchScore": 90.0,
              "experienceScore": 85.0,
              "educationScore": 75.0,
              "matchedSkills": ["Java"],
              "missingSkills": ["Docker"],
              "bonusSkills": ["Kubernetes"],
              "experienceFit": "Strong experience",
              "overallVerdict": "STRONG_MATCH",
              "verdictReason": "Fits core requirements",
              "hiringRecommendation": "RECOMMEND"
            }
            """;
        JsonNode jsonNode = mapper.readTree(mockResponse);
        when(claude.askJson(anyString(), anyString())).thenReturn(jsonNode);

        Map<String, Object> score = service.scoreAgainstJob(
                "Jane Doe resume", "Java Developer", "Job Description", "Java, Docker", 3
        );

        assertEquals(85.0, score.get("matchScore"));
        assertEquals(80.0, score.get("atsScore"));
        assertEquals("STRONG_MATCH", score.get("overallVerdict"));
        assertEquals("RECOMMEND", score.get("hiringRecommendation"));
    }

    @Test
    public void testCompareCandidates() throws Exception {
        String mockResponse = """
            {
              "candidate1Summary": "Strong in Java",
              "candidate2Summary": "Strong in Python",
              "comparisonGrid": [
                {"criteria": "Experience", "candidate1": "5 yrs", "candidate2": "8 yrs", "winner": "CANDIDATE_2"}
              ],
              "candidate1Pros": ["Fast learner"],
              "candidate2Pros": ["Deep knowledge"],
              "candidate1Cons": ["Less experience"],
              "candidate2Cons": ["Slow start"],
              "recommendation": "Recommend Candidate 2"
            }
            """;
        JsonNode jsonNode = mapper.readTree(mockResponse);
        when(claude.askJson(anyString(), anyString())).thenReturn(jsonNode);

        Map<String, Object> comparison = service.compareCandidates(
                "r1 text", "Jane", "r2 text", "John", "Developer", "JD"
        );

        assertEquals("Strong in Java", comparison.get("candidate1Summary"));
        assertEquals("Strong in Python", comparison.get("candidate2Summary"));
        assertEquals("Recommend Candidate 2", comparison.get("recommendation"));
        
        List<?> grid = (List<?>) comparison.get("comparisonGrid");
        assertNotNull(grid);
        assertFalse(grid.isEmpty());
    }
}
