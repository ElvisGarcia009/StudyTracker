package com.studytracker.service;

import com.studytracker.dto.StatsDtos.Dashboard;
import com.studytracker.dto.StatsDtos.DailyStat;
import com.studytracker.dto.StatsDtos.HeatmapDay;
import com.studytracker.dto.StatsDtos.SubjectStat;
import com.studytracker.entity.StudySession;
import com.studytracker.entity.Subject;
import com.studytracker.repository.StudySessionRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Estadísticas del dashboard. Como es una app de un solo usuario, el
 * historial completo cabe en memoria y se agrega aquí en Java.
 */
@ApplicationScoped
public class StatsService {

    private final StudySessionRepository sessionRepository;
    private final SubjectService subjectService;
    private final ScheduleService scheduleService;
    private final TimeService time;

    public StatsService(StudySessionRepository sessionRepository, SubjectService subjectService,
                        ScheduleService scheduleService, TimeService time) {
        this.sessionRepository = sessionRepository;
        this.subjectService = subjectService;
        this.scheduleService = scheduleService;
        this.time = time;
    }

    public Dashboard dashboard() {
        List<StudySession> sessions = sessionRepository.findFinished();
        List<Subject> subjects = subjectService.listEntities();
        Map<DayOfWeek, Long> planned = scheduleService.plannedMinutesByDay();

        LocalDate today = time.today();
        LocalDate weekStart = time.startOfWeek(today);
        Map<LocalDate, Long> secondsByDay = secondsByDay(sessions);

        long weekSeconds = 0;
        for (int i = 0; i < 7; i++) weekSeconds += secondsByDay.getOrDefault(weekStart.plusDays(i), 0L);

        long weekPlanned = planned.values().stream().mapToLong(Long::longValue).sum();
        long weekGoal = weeklyGoalMinutes(subjects);
        long totalSeconds = sessions.stream().mapToLong(s -> s.accumulatedSeconds).sum();

        Double averageFocus = sessions.stream()
                .map(s -> s.focusRating)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .stream().boxed()
                .map(avg -> Math.round(avg * 10) / 10.0)
                .findFirst().orElse(null);

        return new Dashboard(
                secondsByDay.getOrDefault(today, 0L) / 60,
                planned.get(today.getDayOfWeek()),
                weekSeconds / 60,
                weekPlanned,
                weekGoal,
                StreakCalculator.currentStreak(secondsByDay.keySet(), today),
                StreakCalculator.bestStreak(secondsByDay.keySet()),
                totalSeconds / 60,
                sessions.size(),
                averageFocus,
                dailyStats(secondsByDay, planned, today, 7),
                dailyStats(secondsByDay, planned, today, 30),
                bySubject(sessions, subjects, s -> !time.toLocalDate(s.startedAt).isBefore(weekStart), true),
                bySubject(sessions, subjects, s -> true, false));
    }

    public List<HeatmapDay> heatmap(int days) {
        Map<LocalDate, Long> secondsByDay = secondsByDay(sessionRepository.findFinished());
        LocalDate today = time.today();
        List<HeatmapDay> result = new ArrayList<>(days);
        for (int i = days - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            result.add(new HeatmapDay(day, secondsByDay.getOrDefault(day, 0L) / 60));
        }
        return result;
    }

    /** ¿Lo estudiado esta semana alcanza la suma de metas semanales de las materias activas? */
    public boolean isWeeklyGoalMet(List<StudySession> finished) {
        long goal = weeklyGoalMinutes(subjectService.listEntities());
        if (goal <= 0) return false;
        LocalDate weekStart = time.startOfWeek(time.today());
        long weekSeconds = finished.stream()
                .filter(s -> !time.toLocalDate(s.startedAt).isBefore(weekStart))
                .mapToLong(s -> s.accumulatedSeconds)
                .sum();
        return weekSeconds / 60 >= goal;
    }

    private long weeklyGoalMinutes(List<Subject> subjects) {
        return subjects.stream().filter(s -> !s.archived).mapToLong(s -> s.weeklyGoalMinutes).sum();
    }

    private Map<LocalDate, Long> secondsByDay(List<StudySession> sessions) {
        Map<LocalDate, Long> result = new HashMap<>();
        for (StudySession s : sessions) {
            if (s.accumulatedSeconds > 0) {
                result.merge(time.toLocalDate(s.startedAt), s.accumulatedSeconds, Long::sum);
            }
        }
        return result;
    }

    private List<DailyStat> dailyStats(Map<LocalDate, Long> secondsByDay, Map<DayOfWeek, Long> planned,
                                       LocalDate today, int days) {
        List<DailyStat> result = new ArrayList<>(days);
        for (int i = days - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            result.add(new DailyStat(day, secondsByDay.getOrDefault(day, 0L) / 60, planned.get(day.getDayOfWeek())));
        }
        return result;
    }

    /**
     * Minutos por materia. Si includeActiveWithoutTime es true, también aparecen
     * las materias activas sin tiempo (útil para ver el avance contra la meta).
     */
    private List<SubjectStat> bySubject(List<StudySession> sessions, List<Subject> subjects,
                                        Predicate<StudySession> filter, boolean includeActiveWithoutTime) {
        Map<String, Long> seconds = new HashMap<>();
        for (StudySession s : sessions) {
            if (filter.test(s)) seconds.merge(s.subjectId, s.accumulatedSeconds, Long::sum);
        }
        return subjects.stream()
                .filter(sub -> seconds.containsKey(sub.id.toHexString()) || (includeActiveWithoutTime && !sub.archived))
                .map(sub -> new SubjectStat(sub.id.toHexString(), sub.name, sub.color,
                        seconds.getOrDefault(sub.id.toHexString(), 0L) / 60, sub.weeklyGoalMinutes))
                .sorted(Comparator.comparingLong(SubjectStat::minutes).reversed())
                .toList();
    }
}
