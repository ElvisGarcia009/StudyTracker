package com.studytracker.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record StopSessionRequest(
        @Size(max = 2000, message = "Las notas no pueden pasar de 2000 caracteres")
        String notes,

        @Min(value = 1, message = "La concentración va de 1 a 5")
        @Max(value = 5, message = "La concentración va de 1 a 5")
        Integer focusRating
) {}
