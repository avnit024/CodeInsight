package com.codeinsight.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/** AI hints through the Gemini REST API. Hints never include full solutions or code. */
@RestController
@RequestMapping("/api/ai")
public class AiController {
    private final RestTemplate rest = new RestTemplate();
    private final String key;
    private final String model;

    public AiController(@Value("${app.gemini.key}") String key, @Value("${app.gemini.model}") String model) {
        this.key = key;
        this.model = model;
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/hint")
    public ResponseEntity<?> hint(@RequestBody Map<String, String> in) {
        if (key == null || key.isBlank()) {
            return ResponseEntity.status(503).body(Map.of("error",
                    "AI hints are not configured. Set the GEMINI_API_KEY environment variable and restart."));
        }
        boolean big = "approach".equals(in.get("level"));
        String prompt = "You are a coding interview coach. Problem: \"" + in.getOrDefault("title", "") + "\" ("
                + in.getOrDefault("platform", "") + ", " + in.getOrDefault("difficulty", "") + ", topic: "
                + in.getOrDefault("topic", "") + "). "
                + (big ? "Describe the general approach and the key data structure in 3-4 sentences."
                       : "Give ONE short nudge (max 2 sentences) pointing at the key idea.")
                + " Do NOT give the full solution, code, or pseudocode.";
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + key;
            Map<String, Object> body = Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));
            Map<String, Object> res = rest.postForObject(url, body, Map.class);
            List<Map<String, Object>> cands = (List<Map<String, Object>>) res.get("candidates");
            Map<String, Object> content = (Map<String, Object>) cands.get(0).get("content");
            String text = (String) ((Map<String, Object>) ((List<?>) content.get("parts")).get(0)).get("text");
            return ResponseEntity.ok(Map.of("hint", text.trim()));
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("error", "AI request failed: " + e.getMessage()));
        }
    }
}
