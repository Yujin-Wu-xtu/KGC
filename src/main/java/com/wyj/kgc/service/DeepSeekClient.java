package com.wyj.kgc.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class DeepSeekClient {

    @Value("${deepseek.api.key}")
    private String apiKey;

    @Value("${deepseek.model:deepseek-v4-pro}")
    private String deepSeekModel;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DeepSeekClient(ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * Extracts knowledge from the given text using DeepSeek API.
     *
     * @param text The input text to analyze.
     * @return A JSON string containing nodes and relationships.
     */
    public String extractKnowledge(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "{\"nodes\": [], \"relationships\": []}";
        }

        String systemPrompt = "你是一个知识图谱抽取专家。请读取传入的文本，并严格按照 JSON 格式返回结果。" +
                "JSON 必须包含：\n" +
                "1. nodes：节点数组，每个节点有一个 'name' 字段。\n" +
                "2. relationships：关系数组，指明从哪个节点到哪个节点的 'CONTAINS' 关系（或其他合适的关系）。\n" +
                "格式示例：\n" +
                "{\n" +
                "  \"nodes\": [{\"name\": \"数据结构\"}, {\"name\": \"数组\"}],\n" +
                "  \"relationships\": [{\"from\": \"数据结构\", \"to\": \"数组\", \"type\": \"CONTAINS\"}]\n" +
                "}\n" +
                "请只返回纯净的 JSON 字符串，不要包含 Markdown 代码块标记（如 ```json ... ```）。";

        // Construct the request body using a simple map or manually for simplicity to
        // avoid complex DTOs for now
        // But using Jackson is safer for escaping.
        try {
            var requestBodyNode = objectMapper.createObjectNode();
            requestBodyNode.put("model", deepSeekModel);
            requestBodyNode.put("temperature", 0.0); // Predictable output

            var messagesArray = requestBodyNode.putArray("messages");
            var systemMessage = messagesArray.addObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);

            var userMessage = messagesArray.addObject();
            userMessage.put("role", "user");
            userMessage.put("content", text);

            // Optional: enforce JSON format if model supports it, or rely on prompt
            var responseFormat = requestBodyNode.putObject("response_format");
            responseFormat.put("type", "json_object");

            String requestBody = objectMapper.writeValueAsString(requestBodyNode);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.deepseek.com/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode rootNode = objectMapper.readTree(response.body());
                if (rootNode.has("choices") && rootNode.get("choices").isArray()
                        && rootNode.get("choices").size() > 0) {
                    String content = rootNode.get("choices").get(0).get("message").get("content").asText();
                    // Clean up potential markdown code blocks if the model ignores the instruction
                    return cleanJsonOutput(content);
                } else {
                    throw new RuntimeException("DeepSeek API response format invalid: " + response.body());
                }
            } else {
                throw new RuntimeException("DeepSeek API call failed with status: " + response.statusCode() + ", body: "
                        + response.body());
            }

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error calling DeepSeek API", e);
        }
    }

    private String cleanJsonOutput(String content) {
        content = content.trim();
        if (content.startsWith("```json")) {
            content = content.substring(7);
        }
        if (content.startsWith("```")) {
            content = content.substring(3);
        }
        if (content.endsWith("```")) {
            content = content.substring(0, content.length() - 3);
        }
        return content.trim();
    }

    /**
     * 通用聊天接口，用于 AI 助教等场景。
     */
    public String chat(String systemPrompt, String userQuery) {
        return callDeepSeek(systemPrompt, userQuery, 0.7, 1000, null);
    }

    /**
     * JSON 格式聊天接口，用于 AI 出题等需要结构化输出的场景。
     */
    public String chatJson(String prompt) {
        return callDeepSeek(null, prompt, 0.0, 2000, "json_object");
    }

    private String callDeepSeek(String systemPrompt, String userMessage, double temperature, int maxTokens, String responseFormat) {
        try {
            var requestBodyNode = objectMapper.createObjectNode();
            requestBodyNode.put("model", deepSeekModel);
            requestBodyNode.put("temperature", temperature);
            requestBodyNode.put("max_tokens", maxTokens);

            var messagesArray = requestBodyNode.putArray("messages");
            if (systemPrompt != null) {
                var sysMsg = messagesArray.addObject();
                sysMsg.put("role", "system");
                sysMsg.put("content", systemPrompt);
            }
            var userMsg = messagesArray.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);

            if (responseFormat != null) {
                var rf = requestBodyNode.putObject("response_format");
                rf.put("type", responseFormat);
            }

            String requestBody = objectMapper.writeValueAsString(requestBodyNode);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.deepseek.com/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode rootNode = objectMapper.readTree(response.body());
                if (rootNode.has("choices") && rootNode.get("choices").isArray()
                        && rootNode.get("choices").size() > 0) {
                    String content = rootNode.get("choices").get(0).get("message").get("content").asText();
                    return cleanJsonOutput(content);
                } else {
                    throw new RuntimeException("DeepSeek API response format invalid: " + response.body());
                }
            } else {
                throw new RuntimeException("DeepSeek API call failed with status: " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error calling DeepSeek API", e);
        }
    }
}
