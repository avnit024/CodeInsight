package com.codeinsight.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Pure streak logic, kept separate from the controller so it can be unit tested. */
public final class StreakCalculator {
    private StreakCalculator() {}

    /** Consecutive solved days ending today, or yesterday (so a streak isn't lost before you solve today). */
    public static int current(Set<LocalDate> days, LocalDate today) {
        LocalDate d = days.contains(today) ? today : today.minusDays(1);
        int streak = 0;
        while (days.contains(d)) {
            streak++;
            d = d.minusDays(1);
        }
        return streak;
    }

    public static int longest(Set<LocalDate> days) {
        List<LocalDate> sorted = new ArrayList<>(new TreeSet<>(days));
        int best = 0, run = 0;
        LocalDate prev = null;
        for (LocalDate d : sorted) {
            run = (prev != null && prev.plusDays(1).equals(d)) ? run + 1 : 1;
            best = Math.max(best, run);
            prev = d;
        }
        return best;
    }
}
