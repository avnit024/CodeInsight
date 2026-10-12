package com.codeinsight.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Fetches public coding profile stats from third-party APIs. */
@RestController
@RequestMapping("/api/external")
public class ExternalController {
    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/leetcode/{username}")
    public ResponseEntity<?> leetcode(@PathVariable String username) {
        String query = "query getUser($username: String!) { matchedUser(username: $username) "
                + "{ username submitStatsGlobal { acSubmissionNum { difficulty count } } } }";
        Map<String, Object> body = Map.of("query", query, "variables", Map.of("username", username));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Referer", "https://leetcode.com");
        try {
            ResponseEntity<Map> res = restTemplate.exchange("https://leetcode.com/graphql",
                    HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
            return ResponseEntity.ok(res.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("error", "Could not reach LeetCode: " + e.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/atcoder/{user}")
    public ResponseEntity<?> atcoder(@PathVariable String user) {
        try {
            List<Map<String, Object>> h = restTemplate.getForObject(
                    "https://atcoder.jp/users/" + user + "/history/json", List.class);
            if (h == null || h.isEmpty())
                return ResponseEntity.status(404).body(Map.of("error", "No AtCoder contest history found for this user"));
            int max = 0;
            for (Map<String, Object> c : h) max = Math.max(max, ((Number) c.get("NewRating")).intValue());
            int cur = ((Number) h.get(h.size() - 1).get("NewRating")).intValue();
            return ResponseEntity.ok(Map.of("rating", cur, "maxRating", max, "contests", h.size()));
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("error", "Could not reach AtCoder: " + e.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/github/{user}")
    public ResponseEntity<?> github(@PathVariable String user) {
        try {
            HttpHeaders h = new HttpHeaders();
            h.set("User-Agent", "CodeInsight");
            Map<String, Object> d = restTemplate.exchange("https://api.github.com/users/" + user,
                    HttpMethod.GET, new HttpEntity<>(h), Map.class).getBody();
            Map<String, Object> out = new LinkedHashMap<>();
            for (String k : new String[]{"login", "name", "avatar_url", "public_repos", "followers", "following"})
                out.put(k, d.get(k));
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("error", "GitHub user not found or unreachable"));
        }
    }

    /** Best effort: CodeChef has no public API, so this reads the profile page and may break if the page changes. */
    @GetMapping("/codechef/{user}")
    public ResponseEntity<?> codechef(@PathVariable String user) {
        try {
            HttpHeaders h = new HttpHeaders();
            h.set("User-Agent", "Mozilla/5.0");
            String html = restTemplate.exchange("https://www.codechef.com/users/" + user,
                    HttpMethod.GET, new HttpEntity<>(h), String.class).getBody();
            html = html == null ? "" : html;
            Matcher m = Pattern.compile("rating-number\">(\\d+)").matcher(html);
            if (!m.find())
                return ResponseEntity.status(404).body(Map.of("error",
                        "Could not read this CodeChef profile (it may not exist, or CodeChef blocked the request)"));
            Matcher mx = Pattern.compile("Highest Rating (\\d+)").matcher(html);
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("rating", Integer.parseInt(m.group(1)));
            out.put("maxRating", mx.find() ? Integer.parseInt(mx.group(1)) : null);
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("error", "Could not reach CodeChef: " + e.getMessage()));
        }
    }

    @GetMapping("/codeforces/{handle}")
    public ResponseEntity<?> codeforces(@PathVariable String handle) {
        try {
            Map res = restTemplate.getForObject(
                    "https://codeforces.com/api/user.info?handles=" + handle, Map.class);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("error", "Could not reach Codeforces: " + e.getMessage()));
        }
    }
}
