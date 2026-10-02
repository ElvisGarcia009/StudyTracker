package com.studytracker.dto;

import com.studytracker.entity.SessionMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Los minutos de pomodoro son opcionales: si vienen en 0 se usan 25/5/15. */
public record StartSessionRequest(
        @NotBlank(message = "La materia es obligatoria")
        String subjectId,

        @NotNull(message = "El modo es obligatorio")
        SessionMode mode,

        @Min(0) @Max(180) int workMinutes,
        @Min(0) @Max(60) int breakMinutes,
        @Min(0) @Max(120) int longBreakMinutes
) {}
