package com.resumeiq.controller;

import com.resumeiq.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // Send a message and get AI reply
    @PostMapping("/{resumeId}/{jobId}")
    public ResponseEntity<?> send(@PathVariable Long resumeId,
                                   @PathVariable Long jobId,
                                   @RequestBody Map<String,String> req) {
        String message = req.get("message");
        if (message == null || message.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error","Message cannot be empty"));
        try {
            Map<String,Object> result = chatService.chat(resumeId, jobId, message);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // Get full conversation history
    @GetMapping("/{resumeId}/{jobId}")
    public ResponseEntity<?> history(@PathVariable Long resumeId, @PathVariable Long jobId) {
        return ResponseEntity.ok(chatService.getHistory(resumeId, jobId));
    }

    // Clear conversation
    @DeleteMapping("/{resumeId}/{jobId}")
    public ResponseEntity<?> clear(@PathVariable Long resumeId, @PathVariable Long jobId) {
        chatService.clearHistory(resumeId, jobId);
        return ResponseEntity.ok(Map.of("message","Chat cleared"));
    }

    // Quick suggested questions for recruiters
    @GetMapping("/suggestions/{resumeId}/{jobId}")
    public ResponseEntity<?> suggestions(@PathVariable Long resumeId, @PathVariable Long jobId) {
        List<String> questions = List.of(
            "Is this candidate a strong fit for the role?",
            "What are their top 3 strengths?",
            "What skills are they missing?",
            "How does their experience compare to the job requirements?",
            "Would you recommend hiring this candidate? Why?",
            "What interview questions should I ask this candidate?",
            "Can you summarise this candidate's background in 3 sentences?",
            "What alternative roles might suit this candidate better?"
        );
        return ResponseEntity.ok(Map.of("suggestions", questions));
    }
}
