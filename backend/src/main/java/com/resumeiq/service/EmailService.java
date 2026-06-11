package com.resumeiq.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.from:noreply@resumeiq.local}")
    private String from;

    @Value("${app.mail.enabled:false}")
    private boolean enabled;

    @Async
    public void sendStatusUpdate(String toEmail, String candidateName,
                                  String jobTitle, String status,
                                  double matchScore, double atsScore,
                                  String notes, List<String> tips) {
        if (!enabled || toEmail == null || toEmail.isBlank()) return;

        Map<String,String> statusConfig = getStatusConfig(status);
        String statusMessage = getStatusMessage(status, jobTitle);

        Context ctx = new Context();
        ctx.setVariable("candidateName",  candidateName);
        ctx.setVariable("jobTitle",       jobTitle);
        ctx.setVariable("statusLabel",    statusConfig.get("label"));
        ctx.setVariable("statusColor",    statusConfig.get("bg"));
        ctx.setVariable("statusTextColor",statusConfig.get("text"));
        ctx.setVariable("statusMessage",  statusMessage);
        ctx.setVariable("matchScore",     Math.round(matchScore));
        ctx.setVariable("atsScore",       Math.round(atsScore));
        ctx.setVariable("notes",          notes);
        ctx.setVariable("tips",           tips);

        send(toEmail,
             "ResumeIQ — Application Update: " + jobTitle,
             "email/status-update", ctx);
    }

    @Async
    public void sendResumeUploaded(String toEmail, String candidateName,
                                    String jobTitle, double matchScore, double atsScore,
                                    List<String> matchedSkills, List<String> missingSkills,
                                    List<String> suggestions) {
        if (!enabled || toEmail == null || toEmail.isBlank()) return;

        String matchColor = matchScore >= 70 ? "#16A34A" : matchScore >= 45 ? "#D97706" : "#DC2626";

        Context ctx = new Context();
        ctx.setVariable("candidateName",  candidateName);
        ctx.setVariable("jobTitle",       jobTitle);
        ctx.setVariable("matchScore",     Math.round(matchScore));
        ctx.setVariable("atsScore",       Math.round(atsScore));
        ctx.setVariable("matchColor",     matchColor);
        ctx.setVariable("matchedSkills",  matchedSkills);
        ctx.setVariable("missingSkills",  missingSkills);
        ctx.setVariable("suggestions",    suggestions);

        send(toEmail,
             "ResumeIQ — Your Resume Has Been Analysed by AI",
             "email/resume-uploaded", ctx);
    }

    private void send(String to, String subject, String template, Context ctx) {
        try {
            String html = templateEngine.process(template, ctx);
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(msg);
            log.info("Email sent to {} — {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    private Map<String,String> getStatusConfig(String status) {
        return switch (status) {
            case "SELECTED"     -> Map.of("label","✓ Selected",    "bg","#DCFCE7","text","#15803D");
            case "UNDER_REVIEW" -> Map.of("label","Under Review",  "bg","#EDE9FE","text","#7C3AED");
            case "WAITLISTED"   -> Map.of("label","Waitlisted",    "bg","#FEF3C7","text","#D97706");
            case "REJECTED"     -> Map.of("label","Not Shortlisted","bg","#FEE2E2","text","#DC2626");
            default             -> Map.of("label","Applied",       "bg","#DBEAFE","text","#2563EB");
        };
    }

    private String getStatusMessage(String status, String jobTitle) {
        return switch (status) {
            case "SELECTED"     -> "Congratulations! You have been selected for the " + jobTitle + " role. Our team will reach out to you shortly with next steps.";
            case "UNDER_REVIEW" -> "Your application for " + jobTitle + " is currently being reviewed by our recruitment team. We will keep you updated.";
            case "WAITLISTED"   -> "You have been added to our waitlist for " + jobTitle + ". We will contact you if a position becomes available.";
            case "REJECTED"     -> "Thank you for applying to " + jobTitle + ". After careful consideration, we have decided to move forward with other candidates. We encourage you to apply for future openings.";
            default             -> "Your application for " + jobTitle + " has been received and is being processed.";
        };
    }
}
