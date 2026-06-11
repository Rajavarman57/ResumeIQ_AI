package com.resumeiq.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

public class ResumeParserServiceTest {

    private final ResumeParserService parser = new ResumeParserService();

    @Test
    public void testExtractSkills() {
        String text = "Experienced developer skilled in Java, Spring Boot, Python, and leadership.";
        List<String> skills = parser.extractSkills(text);
        
        assertTrue(skills.contains("java"));
        assertTrue(skills.contains("spring"));
        assertTrue(skills.contains("python"));
        assertTrue(skills.contains("leadership"));
        assertFalse(skills.contains("react"));
    }

    @Test
    public void testExtractExperienceYears() {
        String text1 = "Senior developer with 8 years of experience in backend systems.";
        assertEquals(8, parser.extractExperienceYears(text1));

        String text2 = "Worked as a software engineer from 2018 - 2023.";
        assertEquals(5, parser.extractExperienceYears(text2));
    }

    @Test
    public void testExtractEducation() {
        String text = "Qualifications include a B.Tech in Computer Science and an MBA.";
        String edu = parser.extractEducation(text);
        
        assertTrue(edu.contains("B.TECH"));
        assertTrue(edu.contains("MBA"));
    }

    @Test
    public void testExtractEmail() {
        String text = "Contact me at john.doe@example.com or visit my page.";
        assertEquals("john.doe@example.com", parser.extractEmail(text));
    }

    @Test
    public void testExtractPhone() {
        String text = "Reach out at +1 (555) 019-2834 for inquiries.";
        assertEquals("+1 (555) 019-2834", parser.extractPhone(text));
    }

    @Test
    public void testExtractName() {
        String text = "John Doe\nSoftware Engineer\nResume content here...";
        assertEquals("John Doe", parser.extractName(text));
    }
}
