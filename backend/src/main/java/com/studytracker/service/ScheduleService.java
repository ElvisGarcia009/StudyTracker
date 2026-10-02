package com.studytracker.service;

import com.studytracker.dto.ScheduleBlockRequest;
import com.studytracker.dto.ScheduleBlockResponse;
import com.studytracker.entity.ScheduleBlock;
import com.studytracker.entity.Subject;
import com.studytracker.repository.ScheduleBlockRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;

import java.time.DayOfWeek;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class ScheduleService {

    private final ScheduleBlockRepository scheduleRepository;
    private final SubjectService subjectService;

    public ScheduleService(ScheduleBlockRepository scheduleRepository, SubjectService subjectService) {
        this.scheduleRepository = scheduleRepository;
        this.subjectService = subjectService;
    }

    public List<ScheduleBlockResponse> list() {
        return scheduleRepository.listAll().stream()
                .sorted(Comparator.comparing((ScheduleBlock b) -> b.dayOfWeek)
                        .thenComparing(b -> b.startTime == null ? "99:99" : b.startTime))
                .map(ScheduleBlockResponse::from)
                .toList();
    }

    public ScheduleBlockResponse create(ScheduleBlockRequest request) {
        ScheduleBlock block = new ScheduleBlock();
        apply(block, request);
        scheduleRepository.persist(block);
        return ScheduleBlockResponse.from(block);
    }

    public ScheduleBlockResponse update(String id, ScheduleBlockRequest request) {
        ScheduleBlock block = require(id);
        apply(block, request);
        scheduleRepository.update(block);
        return ScheduleBlockResponse.from(block);
    }

    public void delete(String id) {
        scheduleRepository.delete(require(id));
    }

    /** Minutos planificados por día de la semana, ignorando materias archivadas. */
    public Map<DayOfWeek, Long> plannedMinutesByDay() {
        Set<String> activeSubjects = subjectService.listEntities().stream()
                .filter(s -> !s.archived)
                .map(s -> s.id.toHexString())
                .collect(Collectors.toSet());

        Map<DayOfWeek, Long> result = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) result.put(day, 0L);
        for (ScheduleBlock block : scheduleRepository.listAll()) {
            if (activeSubjects.contains(block.subjectId)) {
                result.merge(block.dayOfWeek, (long) block.plannedMinutes, Long::sum);
            }
        }
        return result;
    }

    private ScheduleBlock require(String id) {
        ScheduleBlock block = scheduleRepository.findById(Ids.parse(id));
        if (block == null) throw new NotFoundException("El bloque del plan no existe");
        return block;
    }

    private void apply(ScheduleBlock block, ScheduleBlockRequest request) {
        Subject subject = subjectService.require(request.subjectId());
        block.subjectId = subject.id.toHexString();
        block.dayOfWeek = request.dayOfWeek();
        block.startTime = (request.startTime() == null || request.startTime().isBlank()) ? null : request.startTime();
        block.plannedMinutes = request.plannedMinutes();
    }
}
