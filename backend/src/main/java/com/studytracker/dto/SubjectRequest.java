package com.studytracker.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SubjectRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 60, message = "El nombre no puede pasar de 60 caracteres")
        String name,

        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "El color debe tener formato #RRGGBB")
        String color,

        @Min(value = 0, message = "La meta semanal no puede ser negativa")
        @Max(value = 10080, message = "La meta semanal no puede superar los minutos de una semana")
        int weeklyGoalMinutes,

        boolean archived
) {}
