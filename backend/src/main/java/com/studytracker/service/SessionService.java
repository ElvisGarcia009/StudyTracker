package com.studytracker.service;

import com.studytracker.dto.ManualSessionRequest;
import com.studytracker.dto.SessionResponse;
import com.studytracker.dto.SessionResultResponse;
import com.studytracker.dto.StartSessionRequest;
import com.studytracker.dto.StopSessionRequest;
import com.studytracker.entity.SessionMode;
import com.studytracker.entity.SessionStatus;
import com.studytracker.entity.StudySession;
import com.studytracker.repository.StudySessionRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class SessionService {

    static final int DEFAULT_WORK_MINUTES = 25;
    static final int DEFAULT_BREAK_MINUTES = 5;
    static final int DEFAULT_LONG_BREAK_MINUTES = 15;
    static final int POMODOROS_BEFORE_LONG_BREAK = 4;

    private final StudySessionRepository sessionRepository;
    private final SubjectService subjectService;
    private final AchievementService achievementService;
    private final TimeService time;

    public SessionService(StudySessionRepository sessionRepository, SubjectService subjectService,
                          AchievementService achievementService, TimeService time) {
        this.sessionRepository = sessionRepository;
        this.subjectService = subjectService;
        this.achievementService = achievementService;
        this.time = time;
    }

    /** Historial de sesiones terminadas, de la más reciente a la más antigua. */
    public List<SessionResponse> list(LocalDate from, LocalDate to, String subjectId) {
        Instant now = time.now();
        Instant fromInstant = from != null ? time.startOf(from) : Instant.EPOCH;
        Instant toInstant = to != null ? time.startOf(to.plusDays(1)) : now.plus(Duration.ofDays(1));

        StringBuilder query = new StringBuilder("status = ?1 and startedAt >= ?2 and startedAt < ?3");
        List<Object> params = new ArrayList<>(List.of(SessionStatus.FINISHED.name(), fromInstant, toInstant));
        if (subjectId != null && !subjectId.isBlank()) {
            query.append(" and subjectId = ?4");
            params.add(subjectId);
        }
        return sessionRepository.list(query.toString(), Sort.descending("startedAt"), params.toArray()).stream()
                .map(s -> SessionResponse.from(s, now))
                .toList();
    }

    public Optional<SessionResponse> active() {
        return sessionRepository.findActive().map(s -> SessionResponse.from(s, time.now()));
    }

    public SessionResponse start(StartSessionRequest request) {
        if (sessionRepository.findActive().isPresent()) {
            throw new ClientErrorException("Ya hay una sesión en curso. Termínala antes de empezar otra.",
                    Response.Status.CONFLICT);
        }
        subjectService.require(request.subjectId());

        Instant now = time.now();
        StudySession session = new StudySession();
        session.subjectId = request.subjectId();
        session.mode = request.mode();
        session.status = SessionStatus.RUNNING;
        session.startedAt = now;
        session.lastResumedAt = now;
        if (request.mode() == SessionMode.POMODORO) {
            session.workMinutes = orDefault(request.workMinutes(), DEFAULT_WORK_MINUTES);
            session.breakMinutes = orDefault(request.breakMinutes(), DEFAULT_BREAK_MINUTES);
            session.longBreakMinutes = orDefault(request.longBreakMinutes(), DEFAULT_LONG_BREAK_MINUTES);
        }
        sessionRepository.persist(session);
        return SessionResponse.from(session, now);
    }

    public SessionResponse pause(String id) {
        StudySession session = requireStatus(id, SessionStatus.RUNNING);
        Instant now = time.now();
        freeze(session, now);
        sessionRepository.update(session);
        return SessionResponse.from(session, now);
    }

    public SessionResponse resume(String id) {
        StudySession session = requireStatus(id, SessionStatus.PAUSED);
        Instant now = time.now();
        session.status = SessionStatus.RUNNING;
        session.lastResumedAt = now;
        session.breakEndsAt = null;
        sessionRepository.update(session);
        return SessionResponse.from(session, now);
    }

    /** Cierra un pomodoro: pausa el timer, suma uno al contador y arranca el descanso. */
    public SessionResponse completePomodoro(String id) {
        StudySession session = requireStatus(id, SessionStatus.RUNNING);
        if (session.mode != SessionMode.POMODORO) {
            throw new BadRequestException("La sesión no está en modo Pomodoro");
        }
        Instant now = time.now();
        freeze(session, now);
        session.pomodorosCompleted++;
        session.lastPomodoroAtSeconds = session.accumulatedSeconds;
        boolean longBreak = session.pomodorosCompleted % POMODOROS_BEFORE_LONG_BREAK == 0;
        session.breakEndsAt = now.plus(Duration.ofMinutes(longBreak ? session.longBreakMinutes : session.breakMinutes));
        sessionRepository.update(session);
        return SessionResponse.from(session, now);
    }

    public SessionResultResponse stop(String id, StopSessionRequest request) {
        StudySession session = require(id);
        if (session.status == SessionStatus.FINISHED) {
            throw new ClientErrorException("La sesión ya estaba terminada", Response.Status.CONFLICT);
        }
        Instant now = time.now();
        freeze(session, now);
        session.status = SessionStatus.FINISHED;
        session.endedAt = now;
        session.breakEndsAt = null;
        if (request != null) {
            session.notes = blankToNull(request.notes());
            session.focusRating = request.focusRating();
        }
        sessionRepository.update(session);
        return new SessionResultResponse(SessionResponse.from(session, now), achievementService.evaluateAndUnlock());
    }

    public SessionResultResponse createManual(ManualSessionRequest request) {
        StudySession session = new StudySession();
        applyManual(session, request);
        sessionRepository.persist(session);
        return new SessionResultResponse(SessionResponse.from(session, time.now()), achievementService.evaluateAndUnlock());
    }

    public SessionResultResponse update(String id, ManualSessionRequest request) {
        StudySession session = requireStatus(id, SessionStatus.FINISHED);
        applyManual(session, request);
        sessionRepository.update(session);
        return new SessionResultResponse(SessionResponse.from(session, time.now()), achievementService.evaluateAndUnlock());
    }

    /** Borra una sesión del historial, o descarta la que está en curso. */
    public void delete(String id) {
        sessionRepository.delete(require(id));
    }

    private void applyManual(StudySession session, ManualSessionRequest request) {
        subjectService.require(request.subjectId());
        session.subjectId = request.subjectId();
        session.status = SessionStatus.FINISHED;
        session.mode = request.mode() != null ? request.mode() : SessionMode.FREE;
        session.startedAt = request.startedAt();
        session.accumulatedSeconds = request.durationMinutes() * 60L;
        session.endedAt = request.startedAt().plus(Duration.ofMinutes(request.durationMinutes()));
        session.lastResumedAt = null;
        session.pomodorosCompleted = request.pomodorosCompleted();
        session.notes = blankToNull(request.notes());
        session.focusRating = request.focusRating();
    }

    /** Pasa el tramo que está corriendo al acumulado y deja el timer quieto. */
    private void freeze(StudySession session, Instant now) {
        session.accumulatedSeconds = session.elapsedSeconds(now);
        session.lastResumedAt = null;
        session.status = SessionStatus.PAUSED;
    }

    private StudySession require(String id) {
        StudySession session = sessionRepository.findById(Ids.parse(id));
        if (session == null) throw new NotFoundException("La sesión no existe");
        return session;
    }

    private StudySession requireStatus(String id, SessionStatus expected) {
        StudySession session = require(id);
        if (session.status != expected) {
            throw new ClientErrorException("La sesión está en estado " + session.status + " y se esperaba " + expected,
                    Response.Status.CONFLICT);
        }
        return session;
    }

    private static int orDefault(int value, int fallback) {
        return value > 0 ? value : fallback;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
