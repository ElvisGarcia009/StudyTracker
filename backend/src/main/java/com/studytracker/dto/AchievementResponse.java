package com.studytracker.dto;

import com.studytracker.achievement.Achievement;

import java.time.Instant;

public record AchievementResponse(
        String code,
        String name,
        String description,
        String icon,
        boolean unlocked,
        Instant unlockedAt
) {
    public static AchievementResponse of(Achievement a, Instant unlockedAt) {
        return new AchievementResponse(a.name(), a.title, a.description, a.icon, unlockedAt != null, unlockedAt);
    }
}
