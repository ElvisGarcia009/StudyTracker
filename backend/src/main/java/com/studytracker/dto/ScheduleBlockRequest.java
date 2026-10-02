package com.studytracker.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.DayOfWeek;

public record ScheduleBlockRequest(
        @NotBlank(message = "La materia es obligatoria")
        String subjectId,

        @NotNull(message = "El día es obligatorio")
        DayOfWeek dayOfWeek,

        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "La hora debe tener formato HH:mm")
        String startTime,

        @Min(value = 5, message = "El bloque debe durar al menos 5 minutos")
        @Max(value = 1440, message = "El bloque no puede durar más de un día")
        int plannedMinutes
) {}
