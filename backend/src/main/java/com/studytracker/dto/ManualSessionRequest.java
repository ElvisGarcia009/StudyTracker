package com.studytracker.dto;

import com.studytracker.entity.SessionMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/** Para registrar a mano una sesión que no se cronometró, o para editar una existente. */
public record ManualSessionRequest(
        @NotBlank(message = "La materia es obligatoria")
        String subjectId,

        @NotNull(message = "La fecha de inicio es obligatoria")
        @PastOrPresent(message = "La sesión no puede empezar en el futuro")
        Instant startedAt,

        @Min(value = 1, message = "La sesión debe durar al menos 1 minuto")
        @Max(value = 1440, message = "La sesión no puede durar más de 24 horas")
        int durationMinutes,

        SessionMode mode,

        @Min(0) @Max(100) int pomodorosCompleted,

        @Size(max = 2000, message = "Las notas no pueden pasar de 2000 caracteres")
        String notes,

        @Min(value = 1, message = "La concentración va de 1 a 5")
        @Max(value = 5, message = "La concentración va de 1 a 5")
        Integer focusRating
) {}
