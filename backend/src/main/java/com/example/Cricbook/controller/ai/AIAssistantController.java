package com.example.Cricbook.controller.ai;

import com.example.Cricbook.entity.User;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.UserRepository;
import com.example.Cricbook.service.ai.AIAssistantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai/assistant")
@RequiredArgsConstructor
public class AIAssistantController {

    private final AIAssistantService assistantService;
    private final UserRepository userRepository;

    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> request) {
        
        User user = userRepository.findByEmailOrPhone(userDetails.getUsername())
                .orElseThrow(() -> ApiException.notFound("User account was not found."));

        String message = request.getOrDefault("message", "");
        String conversationId = request.get("conversationId");
        Map<String, Object> reply = assistantService.processMessage(user.getId(), message, conversationId);
        return ResponseEntity.ok(reply);
    }
}
