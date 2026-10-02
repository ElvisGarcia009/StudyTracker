package com.studytracker.resource;

import com.studytracker.dto.AchievementResponse;
import com.studytracker.service.AchievementService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/achievements")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Logros")
public class AchievementResource {

    private final AchievementService achievementService;

    public AchievementResource(AchievementService achievementService) {
        this.achievementService = achievementService;
    }

    @GET
    public List<AchievementResponse> list() {
        return achievementService.list();
    }
}
