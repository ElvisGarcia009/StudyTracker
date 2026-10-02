package com.studytracker.repository;

import com.studytracker.entity.SessionStatus;
import com.studytracker.entity.StudySession;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class StudySessionRepository implements PanacheMongoRepository<StudySession> {

    /** La sesión que está corriendo o en pausa (solo puede haber una). */
    public Optional<StudySession> findActive() {
        return find("status in ?1", List.of(SessionStatus.RUNNING.name(), SessionStatus.PAUSED.name())).firstResultOptional();
    }

    public List<StudySession> findFinished() {
        return list("status", Sort.ascending("startedAt"), SessionStatus.FINISHED.name());
    }

    /** Sesiones terminadas que empezaron en [from, to). */
    public List<StudySession> findFinishedBetween(Instant from, Instant to) {
        return list("status = ?1 and startedAt >= ?2 and startedAt < ?3",
                Sort.ascending("startedAt"), SessionStatus.FINISHED.name(), from, to);
    }

    public long deleteBySubject(String subjectId) {
        return delete("subjectId", subjectId);
    }

}
