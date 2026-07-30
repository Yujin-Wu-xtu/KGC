package com.wyj.kgc.controller;

import com.wyj.kgc.service.DeepSeekClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI 代理控制器
 * 将前端 AI 请求通过后端转发到 DeepSeek，避免 API key 暴露在浏览器端。
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/ai")
public class AIController {

    private final DeepSeekClient deepSeekClient;

    public AIController(DeepSeekClient deepSeekClient) {
        this.deepSeekClient = deepSeekClient;
    }

    /**
     * AI 助教聊天接口
     * POST /api/v1/ai/tutor
     * Body: { "systemPrompt": "...", "userQuery": "..." }
     */
    @PostMapping("/tutor")
    public ResponseEntity<?> tutor(@RequestBody Map<String, String> request) {
        try {
            String systemPrompt = request.getOrDefault("systemPrompt", "你是知微智学的AI智能助教，帮助学生学习课程知识。请使用中文回答。");
            String userQuery = request.getOrDefault("userQuery", "");
            if (userQuery.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "userQuery 不能为空"));
            }
            String content = deepSeekClient.chat(systemPrompt, userQuery);
            return ResponseEntity.ok(Map.of("content", content));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "AI 请求失败: " + e.getMessage()));
        }
    }

    /**
     * AI 出题接口
     * POST /api/v1/ai/quiz
     * Body: { "prompt": "..." }
     */
    @PostMapping("/quiz")
    public ResponseEntity<?> quiz(@RequestBody Map<String, String> request) {
        try {
            String prompt = request.getOrDefault("prompt", "");
            if (prompt.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "prompt 不能为空"));
            }
            String content = deepSeekClient.chatJson(prompt);
            return ResponseEntity.ok(Map.of("content", content));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "AI 出题失败: " + e.getMessage()));
        }
    }
}
