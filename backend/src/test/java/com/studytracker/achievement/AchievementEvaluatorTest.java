package com.studytracker.achievement;

import com.studytracker.entity.SessionStatus;
import com.studytracker.entity.StudySession;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AchievementEvaluatorTest {

    private static final ZoneId ZONE = ZoneId.of("America/Santo_Domingo");

    private static StudySession session(LocalDateTime start, long minutes, int pomodoros) {
        StudySession s = new StudySession();
        s.status = SessionStatus.FINISHED;
        s.startedAt = start.atZone(ZONE).toInstant();
        s.accumulatedSeconds = minutes * 60;
        s.pomodorosCompleted = pomodoros;
        return s;
    }

    private static Set<Achievement> evaluate(List<StudySession> sessions, boolean weeklyGoalMet) {
        return AchievementEvaluator.evaluate(AchievementEvaluator.buildContext(sessions, ZONE, weeklyGoalMet));
    }

    @Test
    void noSessionsUnlockNothing() {
        assertTrue(evaluate(List.of(), false).isEmpty());
    }

    @Test
    void firstSessionIsUnlockedRightAway() {
        Set<Achievement> result = evaluate(List.of(session(LocalDateTime.of(2026, 10, 1, 15, 0), 30, 0)), false);
        assertEquals(Set.of(Achievement.FIRST_SESSION), result);
    }

    @Test
    void hoursAreAccumulatedAcrossSessions() {
        List<StudySession> sessions = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            sessions.add(session(LocalDateTime.of(2026, 9, 1 + i * 2, 15, 0), 110, 0));
        }
        Set<Achievement> result = evaluate(sessions, false);
        assertTrue(result.contains(Achievement.HOURS_10)); // 11 horas
        assertFalse(result.contains(Achievement.HOURS_50));
        assertFalse(result.contains(Achievement.MARATHON)); // ninguna llega a 2 horas
    }

    @Test
    void threeConsecutiveDaysUnlockStreak() {
        List<StudySession> sessions = List.of(
                session(LocalDateTime.of(2026, 10, 1, 10, 0), 20, 0),
                session(LocalDateTime.of(2026, 10, 2, 10, 0), 20, 0),
                session(LocalDateTime.of(2026, 10, 3, 10, 0), 20, 0));
        Set<Achievement> result = evaluate(sessions, false);
        assertTrue(result.contains(Achievement.STREAK_3));
        assertFalse(result.contains(Achievement.STREAK_7));
    }

    @Test
    void pomodorosAreCountedPerDay() {
        List<StudySession> sameDay = List.of(
                session(LocalDateTime.of(2026, 10, 1, 9, 0), 50, 2),
                session(LocalDateTime.of(2026, 10, 1, 14, 0), 50, 2));
        assertTrue(evaluate(sameDay, false).contains(Achievement.POMODORO_4));

        List<StudySession> differentDays = List.of(
                session(LocalDateTime.of(2026, 10, 1, 9, 0), 50, 2),
                session(LocalDateTime.of(2026, 10, 2, 9, 0), 50, 2));
        assertFalse(evaluate(differentDays, false).contains(Achievement.POMODORO_4));
    }

    @Test
    void timeOfDayAchievementsUseLocalHourAndMinimumLength() {
        assertTrue(evaluate(List.of(session(LocalDateTime.of(2026, 10, 1, 6, 0), 20, 0)), false)
                .contains(Achievement.EARLY_BIRD));
        assertTrue(evaluate(List.of(session(LocalDateTime.of(2026, 10, 1, 23, 30), 20, 0)), false)
                .contains(Achievement.NIGHT_OWL));
        // Muy corta: no cuenta
        assertFalse(evaluate(List.of(session(LocalDateTime.of(2026, 10, 1, 6, 0), 5, 0)), false)
                .contains(Achievement.EARLY_BIRD));
    }

    @Test
    void marathonAndWeeklyGoal() {
        Set<Achievement> result = evaluate(List.of(session(LocalDateTime.of(2026, 10, 1, 15, 0), 125, 0)), true);
        assertTrue(result.contains(Achievement.MARATHON));
        assertTrue(result.contains(Achievement.WEEKLY_GOAL));
    }
}
