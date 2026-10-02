package com.studytracker.service;

import com.studytracker.achievement.Achievement;
import com.studytracker.achievement.AchievementContext;
import com.studytracker.achievement.AchievementEvaluator;
import com.studytracker.dto.AchievementResponse;
import com.studytracker.entity.StudySession;
import com.studytracker.entity.UnlockedAchievement;
import com.studytracker.repository.StudySessionRepository;
import com.studytracker.repository.UnlockedAchievementRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class AchievementService {

    private final UnlockedAchievementRepository unlockedRepository;
    private final StudySessionRepository sessionRepository;
    private final StatsService statsService;
    private final TimeService time;

    public AchievementService(UnlockedAchievementRepository unlockedRepository,
                              StudySessionRepository sessionRepository,
                              StatsService statsService,
                              TimeService time) {
        this.unlockedRepository = unlockedRepository;
        this.sessionRepository = sessionRepository;
        this.statsService = statsService;
        this.time = time;
    }

    /** Todos los logros del catálogo, marcando cuáles ya están desbloqueados. */
    public List<AchievementResponse> list() {
        Map<String, Instant> unlocked = unlockedByCode();
        return Arrays.stream(Achievement.values())
                .map(a -> AchievementResponse.of(a, unlocked.get(a.name())))
                .toList();
    }

    /** Revisa el historial y guarda los logros nuevos. Devuelve solo los recién desbloqueados. */
    public List<AchievementResponse> evaluateAndUnlock() {
        List<StudySession> finished = sessionRepository.findFinished();
        AchievementContext context = AchievementEvaluator.buildContext(
                finished, time.zone(), statsService.isWeeklyGoalMet(finished));

        Map<String, Instant> alreadyUnlocked = unlockedByCode();
        Instant now = time.now();
        List<AchievementResponse> newOnes = new ArrayList<>();

        for (Achievement achievement : AchievementEvaluator.evaluate(context)) {
            if (alreadyUnlocked.containsKey(achievement.name())) continue;
            UnlockedAchievement entity = new UnlockedAchievement();
            entity.code = achievement.name();
            entity.unlockedAt = now;
            unlockedRepository.persist(entity);
            newOnes.add(AchievementResponse.of(achievement, now));
        }
        return newOnes;
    }

    private Map<String, Instant> unlockedByCode() {
        return unlockedRepository.listAll().stream()
                .collect(Collectors.toMap(u -> u.code, u -> u.unlockedAt, (a, b) -> a));
    }
}
