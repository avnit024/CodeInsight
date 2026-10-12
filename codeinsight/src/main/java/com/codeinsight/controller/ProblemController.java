package com.codeinsight.controller;

import com.codeinsight.model.Problem;
import com.codeinsight.repository.ProblemRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/problems")
public class ProblemController {
    private static final Set<String> DIFFICULTIES = Set.of("Easy", "Medium", "Hard");
    private static final Set<String> STATUSES = Set.of("Solved", "Attempted", "Todo");

    /** Spaced-repetition gaps in days: 3, 7, 21, 45 (then stays at 45). */
    private static final int[] INTERVALS = {3, 7, 21, 45};

    private final ProblemRepository problemRepository;

    public ProblemController(ProblemRepository problemRepository) {
        this.problemRepository = problemRepository;
    }

    private Long currentUser(HttpServletRequest req) {
        return (Long) req.getAttribute("userId");
    }

    private static boolean has(String s) {
        return s != null && !s.isBlank() && !s.equalsIgnoreCase("All");
    }

    /** List with optional filters: ?search=&difficulty=&status=&platform=&revision=true */
    @GetMapping
    public List<Problem> getAll(@RequestParam(required = false) String search,
                                @RequestParam(required = false) String difficulty,
                                @RequestParam(required = false) String status,
                                @RequestParam(required = false) String platform,
                                @RequestParam(required = false) Boolean revision,
                                HttpServletRequest req) {
        String q = has(search) ? search.toLowerCase() : null;
        return problemRepository.findByUserId(currentUser(req)).stream()
                .filter(p -> q == null
                        || contains(p.getTitle(), q) || contains(p.getTopic(), q) || contains(p.getNotes(), q))
                .filter(p -> !has(difficulty) || difficulty.equalsIgnoreCase(p.getDifficulty()))
                .filter(p -> !has(status) || status.equalsIgnoreCase(p.getStatus()))
                .filter(p -> !has(platform) || platform.equalsIgnoreCase(p.getPlatform()))
                .filter(p -> revision == null || !revision || Boolean.TRUE.equals(p.getNeedsRevision()))
                .sorted(Comparator.comparing(Problem::getId).reversed())
                .toList();
    }

    private static boolean contains(String field, String q) {
        return field != null && field.toLowerCase().contains(q);
    }

    /** Solved problems whose review date is today or earlier. */
    @GetMapping("/due")
    public List<Problem> due(HttpServletRequest req) {
        LocalDate today = LocalDate.now();
        return problemRepository.findByUserId(currentUser(req)).stream()
                .filter(p -> p.getNextReviewDate() != null && !p.getNextReviewDate().isAfter(today))
                .sorted(Comparator.comparing(Problem::getNextReviewDate))
                .toList();
    }

    /** Log a completed review and schedule the next one further out. */
    @PostMapping("/{id}/reviewed")
    public ResponseEntity<?> reviewed(@PathVariable Long id, HttpServletRequest req) {
        Optional<Problem> found = problemRepository.findById(id);
        if (found.isEmpty() || !found.get().getUserId().equals(currentUser(req))) {
            return ResponseEntity.status(404).body(Map.of("error", "Problem not found"));
        }
        Problem p = found.get();
        int n = p.getReviewCount() == null ? 0 : p.getReviewCount();
        p.setReviewCount(n + 1);
        p.setNextReviewDate(LocalDate.now().plusDays(INTERVALS[Math.min(n + 1, INTERVALS.length - 1)]));
        return ResponseEntity.ok(problemRepository.save(p));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Problem in, HttpServletRequest req) {
        Problem p = new Problem();
        p.setUserId(currentUser(req));
        String error = apply(p, in);
        if (error != null) return ResponseEntity.badRequest().body(Map.of("error", error));
        return ResponseEntity.ok(problemRepository.save(p));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Problem in, HttpServletRequest req) {
        Optional<Problem> found = problemRepository.findById(id);
        if (found.isEmpty() || !found.get().getUserId().equals(currentUser(req))) {
            return ResponseEntity.status(404).body(Map.of("error", "Problem not found"));
        }
        Problem p = found.get();
        String error = apply(p, in);
        if (error != null) return ResponseEntity.badRequest().body(Map.of("error", error));
        return ResponseEntity.ok(problemRepository.save(p));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpServletRequest req) {
        Optional<Problem> found = problemRepository.findById(id);
        if (found.isEmpty() || !found.get().getUserId().equals(currentUser(req))) {
            return ResponseEntity.status(404).body(Map.of("error", "Problem not found"));
        }
        problemRepository.delete(found.get());
        return ResponseEntity.ok(Map.of("message", "Deleted"));
    }

    /** Validates and copies fields from the request onto the entity. Returns an error message or null. */
    private String apply(Problem p, Problem in) {
        String title = in.getTitle() == null ? "" : in.getTitle().trim();
        if (title.isEmpty()) return "Title is required";
        if (title.length() > 200) return "Title is too long (max 200 characters)";

        String difficulty = in.getDifficulty() == null || in.getDifficulty().isBlank() ? "Medium" : in.getDifficulty();
        String status = in.getStatus() == null || in.getStatus().isBlank() ? "Todo" : in.getStatus();
        if (!DIFFICULTIES.contains(difficulty)) return "Difficulty must be Easy, Medium or Hard";
        if (!STATUSES.contains(status)) return "Status must be Solved, Attempted or Todo";
        if (in.getNotes() != null && in.getNotes().length() > 1000) return "Notes are too long (max 1000 characters)";

        p.setTitle(title);
        p.setPlatform(in.getPlatform() == null || in.getPlatform().isBlank() ? "Other" : in.getPlatform().trim());
        p.setDifficulty(difficulty);
        p.setTopic(in.getTopic() == null ? null : in.getTopic().trim());
        p.setLink(in.getLink() == null ? null : in.getLink().trim());
        p.setNotes(in.getNotes());
        p.setNeedsRevision(Boolean.TRUE.equals(in.getNeedsRevision()));

        // Solved date: keep an existing one, otherwise stamp today; clear it if no longer solved
        if (status.equals("Solved")) {
            if (p.getSolvedDate() == null) {
                p.setSolvedDate(in.getSolvedDate() != null ? in.getSolvedDate() : LocalDate.now());
            }
            if (p.getNextReviewDate() == null) {   // first review 3 days after solving
                p.setReviewCount(0);
                p.setNextReviewDate(LocalDate.now().plusDays(INTERVALS[0]));
            }
        } else {
            p.setSolvedDate(null);
            p.setNextReviewDate(null);
        }
        p.setStatus(status);
        return null;
    }
}
