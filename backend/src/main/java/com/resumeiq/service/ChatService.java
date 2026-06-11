package com.resumeiq.service;

import com.resumeiq.model.*;
import com.resumeiq.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final GeminiAIService gemini;
    private final ChatMessageRepository chatRepo;
    private final ResumeRepository resumeRepo;
    private final CandidateScoreRepository scoreRepo;

    private static final String SYSTEM_CHAT =
        "You are an expert AI recruitment assistant within the ResumeIQ platform. " +
        "You have access to a specific candidate's resume data and their job match analysis. " +
        "Answer questions about this candidate clearly, concisely, and professionally. " +
        "You can discuss their skills, experience, education, match scores, skill gaps, " +
        "and give hiring recommendations. Be honest and specific. " +
        "If asked something outside the candidate's data, say so politely.";

    public Map<String,Object> chat(Long resumeId, Long jobId, String userMessage) {

        Resume resume = resumeRepo.findById(resumeId)
                .orElseThrow(() -> new RuntimeException("Resume not found"));
        CandidateScore score = scoreRepo.findByResumeIdAndJobRoleId(resumeId, jobId)
                .orElse(null);

        // Build context about this candidate
        String context = buildContext(resume, score);

        // Build conversation history for multi-turn chat
        List<ChatMessage> history = chatRepo.findByResumeIdAndJobIdOrderBySentAtAsc(resumeId, jobId);
        String conversationHistory = history.stream()
                .map(m -> (m.getSender() == ChatMessage.Sender.USER ? "Recruiter: " : "AI: ") + m.getContent())
                .collect(Collectors.joining("\n"));

        // Build full prompt
        String fullPrompt = """
            CANDIDATE CONTEXT:
            %s

            CONVERSATION SO FAR:
            %s

            RECRUITER QUESTION:
            %s

            Please answer the recruiter's question based on the candidate's data above.
            Be specific, helpful, and professional. Keep response under 200 words unless detail is needed.
            """.formatted(context, conversationHistory.isEmpty() ? "(New conversation)" : conversationHistory, userMessage);

        // Call Gemini
        String aiResponse = gemini.ask(SYSTEM_CHAT, fullPrompt);
        if (aiResponse == null) aiResponse = "I'm sorry, I'm unable to process your request right now. Please try again.";

        // Persist both messages
        chatRepo.save(ChatMessage.builder()
                .resumeId(resumeId).jobId(jobId)
                .sender(ChatMessage.Sender.USER)
                .content(userMessage).build());

        chatRepo.save(ChatMessage.builder()
                .resumeId(resumeId).jobId(jobId)
                .sender(ChatMessage.Sender.AI)
                .content(aiResponse).build());

        return Map.of(
                "response",    aiResponse,
                "resumeId",    resumeId,
                "jobId",       jobId,
                "candidateName", resume.getCandidateName()
        );
    }

    public List<Map<String,Object>> getHistory(Long resumeId, Long jobId) {
        return chatRepo.findByResumeIdAndJobIdOrderBySentAtAsc(resumeId, jobId)
                .stream().map(m -> {
                    Map<String,Object> map = new LinkedHashMap<>();
                    map.put("id",      m.getId());
                    map.put("sender",  m.getSender().name());
                    map.put("content", m.getContent());
                    map.put("sentAt",  m.getSentAt().toString());
                    return map;
                }).collect(Collectors.toList());
    }

    public void clearHistory(Long resumeId, Long jobId) {
        chatRepo.deleteByResumeIdAndJobId(resumeId, jobId);
    }

    private String buildContext(Resume resume, CandidateScore score) {
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(resume.getCandidateName()).append("\n");
        sb.append("Email: ").append(resume.getCandidateEmail()).append("\n");
        sb.append("Experience: ").append(resume.getExperienceYears()).append(" years\n");
        sb.append("Education: ").append(resume.getEducation()).append("\n");
        sb.append("Skills: ").append(resume.getExtractedSkills()).append("\n");

        if (score != null) {
            sb.append("Job Role: ").append(score.getJobRole().getTitle()).append("\n");
            sb.append("Match Score: ").append(score.getMatchScore()).append("%\n");
            sb.append("ATS Score: ").append(score.getAtsScore()).append("%\n");
            sb.append("Skill Gap: ").append(score.getSkillGap()).append("\n");
            if (score.getSuggestions() != null) {
                sb.append("AI Assessment: ").append(score.getSuggestions(), 0,
                        Math.min(500, score.getSuggestions().length())).append("\n");
            }
        }

        if (resume.getRawText() != null) {
            sb.append("Resume Excerpt: ").append(resume.getRawText(), 0,
                    Math.min(1500, resume.getRawText().length())).append("\n");
        }
        return sb.toString();
    }
}
