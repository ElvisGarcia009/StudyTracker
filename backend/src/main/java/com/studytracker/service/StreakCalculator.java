package com.studytracker.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

/** Calcula rachas de días consecutivos estudiando. */
public final class StreakCalculator {

    private StreakCalculator() {}

    /**
     * Racha actual: días seguidos que terminan hoy. Si hoy todavía no se ha
     * estudiado pero ayer sí, la racha sigue viva y se cuenta hasta ayer.
     */
    public static int currentStreak(Set<LocalDate> studiedDays, LocalDate today) {
        LocalDate day = studiedDays.contains(today) ? today : today.minusDays(1);
        int streak = 0;
        while (studiedDays.contains(day)) {
            streak++;
            day = day.minusDays(1);
        }
        return streak;
    }

    /** La racha más larga de todo el historial. */
    public static int bestStreak(Collection<LocalDate> studiedDays) {
        int best = 0;
        int current = 0;
        LocalDate previous = null;
        for (LocalDate day : new TreeSet<>(studiedDays)) {
            current = (previous != null && previous.plusDays(1).equals(day)) ? current + 1 : 1;
            best = Math.max(best, current);
            previous = day;
        }
        return best;
    }
}
