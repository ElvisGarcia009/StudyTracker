package com.studytracker.service;

import com.studytracker.dto.SubjectRequest;
import com.studytracker.dto.SubjectResponse;
import com.studytracker.entity.Subject;
import com.studytracker.repository.GoalRepository;
import com.studytracker.repository.ScheduleBlockRepository;
import com.studytracker.repository.StudySessionRepository;
import com.studytracker.repository.SubjectRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;

import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class SubjectService {

    /** Colores que se asignan en orden cuando no se elige uno. */
    private static final List<String> PALETTE = List.of(
            "#6366f1", "#10b981", "#f59e0b", "#ef4444", "#06b6d4",
            "#8b5cf6", "#ec4899", "#84cc16", "#f97316", "#14b8a6");

    private final SubjectRepository subjectRepository;
    private final ScheduleBlockRepository scheduleRepository;
    private final StudySessionRepository sessionRepository;
    private final GoalRepository goalRepository;

    public SubjectService(SubjectRepository subjectRepository,
                          ScheduleBlockRepository scheduleRepository,
                          StudySessionRepository sessionRepository,
                          GoalRepository goalRepository) {
        this.subjectRepository = subjectRepository;
        this.scheduleRepository = scheduleRepository;
        this.sessionRepository = sessionRepository;
        this.goalRepository = goalRepository;
    }

    public List<SubjectResponse> list() {
        return subjectRepository.listAll(Sort.ascending("name")).stream()
                .map(SubjectResponse::from)
                .toList();
    }

    public List<Subject> listEntities() {
        return subjectRepository.listAll();
    }

    public Subject require(String id) {
        Subject subject = subjectRepository.findById(Ids.parse(id));
        if (subject == null) throw new NotFoundException("La materia no existe");
        return subject;
    }

    public SubjectResponse create(SubjectRequest request) {
        Subject subject = new Subject();
        apply(subject, request);
        if (subject.color == null) {
            subject.color = PALETTE.get((int) (subjectRepository.count() % PALETTE.size()));
        }
        subject.createdAt = Instant.now();
        subjectRepository.persist(subject);
        return SubjectResponse.from(subject);
    }

    public SubjectResponse update(String id, SubjectRequest request) {
        Subject subject = require(id);
        apply(subject, request);
        subjectRepository.update(subject);
        return SubjectResponse.from(subject);
    }

    /** Borra la materia junto con su plan, sus sesiones y sus metas. */
    public void delete(String id) {
        Subject subject = require(id);
        scheduleRepository.delete("subjectId", id);
        sessionRepository.deleteBySubject(id);
        goalRepository.delete("subjectId", id);
        subjectRepository.delete(subject);
    }

    private void apply(Subject subject, SubjectRequest request) {
        subject.name = request.name().trim();
        if (request.color() != null) subject.color = request.color();
        subject.weeklyGoalMinutes = request.weeklyGoalMinutes();
        subject.archived = request.archived();
    }
}
