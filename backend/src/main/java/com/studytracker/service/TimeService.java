package com.studytracker.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

/**
 * Centraliza la zona horaria del usuario: los "días" de las estadísticas
 * se calculan en su hora local, no en UTC.
 */
@ApplicationScoped
public class TimeService {

    private final ZoneId zone;

    public TimeService(@ConfigProperty(name = "studytracker.timezone") String timezone) {
        this.zone = ZoneId.of(timezone);
    }

    public ZoneId zone() {
        return zone;
    }

    public Instant now() {
        return Instant.now();
    }

    public LocalDate today() {
        return LocalDate.now(zone);
    }

    public LocalDate toLocalDate(Instant instant) {
        return instant.atZone(zone).toLocalDate();
    }

    /** Primer instante del día indicado en la zona del usuario. */
    public Instant startOf(LocalDate date) {
        return date.atStartOfDay(zone).toInstant();
    }

    public LocalDate startOfWeek(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
