package com.studytracker.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StreakCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 2);

    @Test
    void noStudyMeansNoStreak() {
        assertEquals(0, StreakCalculator.currentStreak(Set.of(), TODAY));
        assertEquals(0, StreakCalculator.bestStreak(Set.of()));
    }

    @Test
    void streakEndingTodayIsCounted() {
        Set<LocalDate> days = Set.of(TODAY, TODAY.minusDays(1), TODAY.minusDays(2));
        assertEquals(3, StreakCalculator.currentStreak(days, TODAY));
    }

    @Test
    void streakStaysAliveIfOnlyYesterdayWasStudied() {
        Set<LocalDate> days = Set.of(TODAY.minusDays(1), TODAY.minusDays(2));
        assertEquals(2, StreakCalculator.currentStreak(days, TODAY));
    }

    @Test
    void streakBreaksAfterAMissedDay() {
        Set<LocalDate> days = Set.of(TODAY.minusDays(2), TODAY.minusDays(3));
        assertEquals(0, StreakCalculator.currentStreak(days, TODAY));
    }

    @Test
    void bestStreakFindsTheLongestRun() {
        Set<LocalDate> days = Set.of(
                TODAY.minusDays(10), TODAY.minusDays(9), TODAY.minusDays(8), TODAY.minusDays(7),
                TODAY.minusDays(3), TODAY.minusDays(2),
                TODAY);
        assertEquals(4, StreakCalculator.bestStreak(days));
    }
}
