package com.codeinsight.controller;

import com.codeinsight.model.Problem;
import com.codeinsight.repository.ProblemRepository;
import com.codeinsight.service.StreakCalculator;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stats")
public class StatsController {
    private final ProblemRepository problemRepository;

    public StatsController(ProblemRepository problemRepository) {
        this.problemRepository = problemRepository;
    }

    @GetMapping
    public Map<String, Object> stats(HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("userId");
        List<Problem> all = problemRepository.findByUserId(userId);
        List<Problem> solved = all.stream()
                .filter(p -> "Solved".equalsIgnoreCase(p.getStatus()))
                .toList();

        // Solved dates (older v1 problems may have none) -> counts per day
        Map<LocalDate, Long> perDay = solved.stream()
                .map(Problem::getSolvedDate).filter(Objects::nonNull)
                .collect(Collectors.groupingBy(d -> d, Collectors.counting()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", all.size());
        result.put("solved", solved.size());
        result.put("needsRevision", all.stream().filter(p -> Boolean.TRUE.equals(p.getNeedsRevision())).count());
        result.put("currentStreak", StreakCalculator.current(perDay.keySet(), LocalDate.now()));
        result.put("longestStreak", StreakCalculator.longest(perDay.keySet()));
        result.put("last14Days", lastDays(perDay, 14));
        result.put("last119Days", lastDays(perDay, 119));
        result.put("byDifficulty", countBy(solved, Problem::getDifficulty));
        result.put("byTopic", countBy(solved, Problem::getTopic));
        result.put("byPlatform", countBy(solved, Problem::getPlatform));
        return result;
    }

    private Map<String, Long> lastDays(Map<LocalDate, Long> perDay, int n) {
        Map<String, Long> out = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (int i = n - 1; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            out.put(d.toString(), perDay.getOrDefault(d, 0L));
        }
        return out;
    }

    private Map<String, Long> countBy(List<Problem> list, Function<Problem, String> field) {
        return list.stream().collect(Collectors.groupingBy(
                p -> {
                    String v = field.apply(p);
                    return (v == null || v.isBlank()) ? "Unknown" : v;
                },
                LinkedHashMap::new,
                Collectors.counting()));
    }
}
