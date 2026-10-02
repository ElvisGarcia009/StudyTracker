package com.studytracker.achievement;

import java.util.function.Predicate;

/**
 * Catálogo de logros. Para agregar uno nuevo basta con sumar una constante
 * con su condición; el frontend lo mostrará automáticamente.
 */
public enum Achievement {

    FIRST_SESSION("Primer paso", "Completa tu primera sesión de estudio", "🎯",
            c -> c.sessionCount() >= 1),
    HOURS_10("Calentando motores", "Acumula 10 horas de estudio", "🔥",
            c -> c.totalSeconds() >= 10 * 3600),
    HOURS_50("Dedicación total", "Acumula 50 horas de estudio", "📚",
            c -> c.totalSeconds() >= 50 * 3600),
    HOURS_100("Centenario", "Acumula 100 horas de estudio", "🏆",
            c -> c.totalSeconds() >= 100 * 3600),
    STREAK_3("Constancia", "Estudia 3 días seguidos", "📅",
            c -> c.bestStreak() >= 3),
    STREAK_7("Semana perfecta", "Estudia 7 días seguidos", "⚡",
            c -> c.bestStreak() >= 7),
    STREAK_30("Imparable", "Estudia 30 días seguidos", "👑",
            c -> c.bestStreak() >= 30),
    POMODORO_4("Tomatero", "Completa 4 pomodoros en un mismo día", "🍅",
            c -> c.maxPomodorosInDay() >= 4),
    WEEKLY_GOAL("Meta cumplida", "Alcanza tu meta semanal de horas", "✅",
            AchievementContext::weeklyGoalMet),
    MARATHON("Maratón", "Haz una sesión de 2 horas o más", "🏃",
            c -> c.longestSessionSeconds() >= 2 * 3600),
    EARLY_BIRD("Madrugador", "Estudia al menos 15 minutos antes de las 7:00 am", "🌅",
            AchievementContext::hasEarlySession),
    NIGHT_OWL("Búho nocturno", "Estudia al menos 15 minutos después de las 11:00 pm", "🦉",
            AchievementContext::hasLateSession);

    public final String title;
    public final String description;
    public final String icon;
    private final Predicate<AchievementContext> condition;

    Achievement(String title, String description, String icon, Predicate<AchievementContext> condition) {
        this.title = title;
        this.description = description;
        this.icon = icon;
        this.condition = condition;
    }

    public boolean isMet(AchievementContext context) {
        return condition.test(context);
    }
}
