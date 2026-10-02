package com.studytracker.dto;

import com.studytracker.entity.GoalPeriod;

import java.time.LocalDate;

public record GoalResponse(
        String id,
        String title,
        String subjectId,
        int targetMinutes,
        GoalPeriod period,
        LocalDate deadline,
        LocalDate periodStart,
        LocalDate periodEnd,
        long progressMinutes,
        int percent,
        boolean completed,
        boolean expired
) {}
