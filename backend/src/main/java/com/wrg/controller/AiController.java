package com.wrg.controller;

import com.wrg.dto.AiChatRequest;
import com.wrg.service.AiAssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Good-to-have AI Chat Assistant - manager only (enforced in SecurityConfig). */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiAssistantService aiAssistantService;

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(@Valid @RequestBody AiChatRequest request) {
        String answer = aiAssistantService.ask(request.getQuestion());
        return ResponseEntity.ok(Map.of("answer", answer));
    }
}
