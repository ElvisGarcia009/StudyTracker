package com.studytracker.dto;

import com.studytracker.entity.GoalPeriod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record GoalRequest(
        @NotBlank(message = "El título es obligatorio")
        @Size(max = 100, message = "El título no puede pasar de 100 caracteres")
        String title,

        /** Vacío o null = cuenta todas las materias. */
        String subjectId,

        @Min(value = 1, message = "La meta debe ser de al menos 1 minuto")
        int targetMinutes,

        @NotNull(message = "El periodo es obligatorio")
        GoalPeriod period,

        LocalDate deadline
) {}
