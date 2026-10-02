package com.studytracker.resource;

import com.studytracker.dto.GoalRequest;
import com.studytracker.dto.GoalResponse;
import com.studytracker.service.GoalService;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/goals")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Metas")
public class GoalResource {

    private final GoalService goalService;

    public GoalResource(GoalService goalService) {
        this.goalService = goalService;
    }

    @GET
    public List<GoalResponse> list() {
        return goalService.list();
    }

    @POST
    public Response create(@Valid GoalRequest request) {
        return Response.status(Response.Status.CREATED).entity(goalService.create(request)).build();
    }

    @PUT
    @Path("/{id}")
    public GoalResponse update(@PathParam("id") String id, @Valid GoalRequest request) {
        return goalService.update(id, request);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id) {
        goalService.delete(id);
        return Response.noContent().build();
    }
}
