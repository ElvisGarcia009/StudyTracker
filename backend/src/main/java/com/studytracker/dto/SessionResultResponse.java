package com.studytracker.dto;

import java.util.List;

/** Respuesta al cerrar o registrar una sesión: incluye los logros que se acaban de desbloquear. */
public record SessionResultResponse(
        SessionResponse session,
        List<AchievementResponse> newAchievements
) {}
