package com.bat.uberlandia.dashboard.controller;

import com.bat.uberlandia.dashboard.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/chatbot")
    public Map<String, Object> chat(@RequestBody Map<String, String> request) {
        String message = request.getOrDefault("message", "").trim();
        if (message.isEmpty()) {
            return Map.of("text", "Por favor, digite uma mensagem.");
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null && auth.isAuthenticated()) ? auth.getName() : "visitante";

        return chatbotService.processMessage(message, username);
    }
}
