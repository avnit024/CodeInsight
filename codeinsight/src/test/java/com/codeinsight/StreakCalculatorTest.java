package com.codeinsight;

import com.codeinsight.service.StreakCalculator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StreakCalculatorTest {
    private final LocalDate today = LocalDate.of(2026, 10, 6);

    @Test
    void streakCountsConsecutiveDaysEndingToday() {
        Set<LocalDate> days = Set.of(today, today.minusDays(1), today.minusDays(2));
        assertEquals(3, StreakCalculator.current(days, today));
    }

    @Test
    void streakSurvivesIfOnlyYesterdayWasSolved() {
        assertEquals(1, StreakCalculator.current(Set.of(today.minusDays(1)), today));
    }

    @Test
    void streakBreaksAfterAGap() {
        assertEquals(0, StreakCalculator.current(Set.of(today.minusDays(3)), today));
    }

    @Test
    void longestStreakFindsBestRun() {
        Set<LocalDate> days = Set.of(today, today.minusDays(1), today.minusDays(5),
                today.minusDays(6), today.minusDays(7), today.minusDays(8));
        assertEquals(4, StreakCalculator.longest(days));
    }

    @Test
    void emptyHistoryIsZero() {
        assertEquals(0, StreakCalculator.current(Set.of(), today));
        assertEquals(0, StreakCalculator.longest(Set.of()));
    }
}
