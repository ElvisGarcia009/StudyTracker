package com.studytracker.service;

import com.studytracker.dto.GoalRequest;
import com.studytracker.dto.GoalResponse;
import com.studytracker.entity.Goal;
import com.studytracker.entity.GoalPeriod;
import com.studytracker.entity.StudySession;
import com.studytracker.repository.GoalRepository;
import com.studytracker.repository.StudySessionRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@ApplicationScoped
public class GoalService {

    private final GoalRepository goalRepository;
    private final StudySessionRepository sessionRepository;
    private final SubjectService subjectService;
    private final TimeService time;

    public GoalService(GoalRepository goalRepository, StudySessionRepository sessionRepository,
                       SubjectService subjectService, TimeService time) {
        this.goalRepository = goalRepository;
        this.sessionRepository = sessionRepository;
        this.subjectService = subjectService;
        this.time = time;
    }

    public List<GoalResponse> list() {
        return goalRepository.listAll(Sort.ascending("createdAt")).stream()
                .map(this::toResponse)
                .toList();
    }

    public GoalResponse create(GoalRequest request) {
        Goal goal = new Goal();
        goal.createdAt = time.now();
        apply(goal, request);
        goalRepository.persist(goal);
        return toResponse(goal);
    }

    public GoalResponse update(String id, GoalRequest request) {
        Goal goal = require(id);
        apply(goal, request);
        goalRepository.update(goal);
        return toResponse(goal);
    }

    public void delete(String id) {
        goalRepository.delete(require(id));
    }

    private GoalResponse toResponse(Goal goal) {
        LocalDate today = time.today();
        LocalDate start;
        LocalDate end;
        switch (goal.period) {
            case WEEKLY -> {
                start = time.startOfWeek(today);
                end = start.plusDays(6);
            }
            case MONTHLY -> {
                start = today.withDayOfMonth(1);
                end = today.with(TemporalAdjusters.lastDayOfMonth());
            }
            default -> {
                start = time.toLocalDate(goal.createdAt);
                end = goal.deadline;
            }
        }

        Instant from = time.startOf(start);
        Instant to = time.startOf(end.plusDays(1));
        long seconds = sessionRepository.findFinishedBetween(from, to).stream()
                .filter(s -> goal.subjectId == null || goal.subjectId.equals(s.subjectId))
                .mapToLong((StudySession s) -> s.accumulatedSeconds)
                .sum();

        long minutes = seconds / 60;
        int percent = (int) Math.min(100, minutes * 100 / goal.targetMinutes);
        boolean completed = minutes >= goal.targetMinutes;
        boolean expired = goal.period == GoalPeriod.UNTIL_DATE && today.isAfter(end) && !completed;

        return new GoalResponse(goal.id.toHexString(), goal.title, goal.subjectId, goal.targetMinutes,
                goal.period, goal.deadline, start, end, minutes, percent, completed, expired);
    }

    private Goal require(String id) {
        Goal goal = goalRepository.findById(Ids.parse(id));
        if (goal == null) throw new NotFoundException("La meta no existe");
        return goal;
    }

    private void apply(Goal goal, GoalRequest request) {
        if (request.period() == GoalPeriod.UNTIL_DATE && request.deadline() == null) {
            throw new BadRequestException("Las metas con fecha límite necesitan una fecha");
        }
        String subjectId = request.subjectId() == null || request.subjectId().isBlank() ? null : request.subjectId();
        if (subjectId != null) subjectService.require(subjectId);

        goal.title = request.title().trim();
        goal.subjectId = subjectId;
        goal.targetMinutes = request.targetMinutes();
        goal.period = request.period();
        goal.deadline = request.period() == GoalPeriod.UNTIL_DATE ? request.deadline() : null;
    }
}
