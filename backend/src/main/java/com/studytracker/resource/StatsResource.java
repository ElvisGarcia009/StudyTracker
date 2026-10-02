package com.studytracker.resource;

import com.studytracker.dto.StatsDtos.Dashboard;
import com.studytracker.dto.StatsDtos.HeatmapDay;
import com.studytracker.service.StatsService;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/stats")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Estadísticas")
public class StatsResource {

    private final StatsService statsService;

    public StatsResource(StatsService statsService) {
        this.statsService = statsService;
    }

    @GET
    @Path("/dashboard")
    public Dashboard dashboard() {
        return statsService.dashboard();
    }

    @GET
    @Path("/heatmap")
    public List<HeatmapDay> heatmap(@QueryParam("days") @DefaultValue("365") int days) {
        if (days < 1 || days > 1000) throw new BadRequestException("days debe estar entre 1 y 1000");
        return statsService.heatmap(days);
    }
}
