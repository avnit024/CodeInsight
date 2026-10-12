package com.codeinsight.controller;

import com.codeinsight.model.Problem;
import com.codeinsight.repository.ProblemRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/** Imports recent accepted LeetCode submissions as Solved problems (LeetCode's API is unofficial and may change). */
@RestController
@RequestMapping("/api/sync")
public class SyncController {
    private static final String URL = "https://leetcode.com/graphql";
    private final RestTemplate rest = new RestTemplate();
    private final ProblemRepository problems;

    public SyncController(ProblemRepository problems) {
        this.problems = problems;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> gql(String query, Map<String, Object> vars) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("Referer", "https://leetcode.com");
        ResponseEntity<Map> r = rest.exchange(URL, HttpMethod.POST,
                new HttpEntity<>(Map.of("query", query, "variables", vars), h), Map.class);
        Map<String, Object> body = r.getBody();
        return body == null ? Map.of() : (Map<String, Object>) body.get("data");
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/leetcode/{username}")
    public ResponseEntity<?> leetcode(@PathVariable String username, HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("userId");
        try {
            Map<String, Object> data = gql("query($u:String!){recentAcSubmissionList(username:$u,limit:20){title titleSlug timestamp}}",
                    Map.of("u", username));
            List<Map<String, String>> subs = data == null ? null : (List<Map<String, String>>) data.get("recentAcSubmissionList");
            if (subs == null) return ResponseEntity.status(404).body(Map.of("error", "LeetCode user not found"));

            Set<String> have = problems.findByUserId(userId).stream()
                    .map(p -> p.getTitle().toLowerCase()).collect(Collectors.toCollection(HashSet::new));
            int added = 0, skipped = 0;
            for (Map<String, String> s : subs) {
                String title = s.get("title");
                if (!have.add(title.toLowerCase())) { skipped++; continue; }

                Problem p = new Problem();
                p.setUserId(userId);
                p.setTitle(title);
                p.setPlatform("LeetCode");
                p.setStatus("Solved");
                p.setLink("https://leetcode.com/problems/" + s.get("titleSlug") + "/");
                p.setSolvedDate(Instant.ofEpochSecond(Long.parseLong(s.get("timestamp")))
                        .atZone(ZoneId.systemDefault()).toLocalDate());
                p.setDifficulty("Medium");
                p.setTopic("Unknown");
                try {   // look up real difficulty and first topic tag
                    Map<String, Object> q = gql("query($s:String!){question(titleSlug:$s){difficulty topicTags{name}}}",
                            Map.of("s", s.get("titleSlug")));
                    Map<String, Object> question = (Map<String, Object>) q.get("question");
                    p.setDifficulty((String) question.get("difficulty"));
                    List<Map<String, String>> tags = (List<Map<String, String>>) question.get("topicTags");
                    if (tags != null && !tags.isEmpty()) p.setTopic(tags.get(0).get("name"));
                } catch (Exception ignored) { /* keep defaults */ }
                p.setReviewCount(0);
                p.setNextReviewDate(LocalDate.now().plusDays(3));
                p.setNeedsRevision(false);
                problems.save(p);
                added++;
            }
            return ResponseEntity.ok(Map.of("added", added, "skipped", skipped));
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("error", "Could not reach LeetCode: " + e.getMessage()));
        }
    }
}
