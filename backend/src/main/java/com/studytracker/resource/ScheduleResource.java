package com.studytracker.resource;

import com.studytracker.dto.ScheduleBlockRequest;
import com.studytracker.dto.ScheduleBlockResponse;
import com.studytracker.service.ScheduleService;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/schedule")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Plan semanal")
public class ScheduleResource {

    private final ScheduleService scheduleService;

    public ScheduleResource(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GET
    public List<ScheduleBlockResponse> list() {
        return scheduleService.list();
    }

    @POST
    public Response create(@Valid ScheduleBlockRequest request) {
        return Response.status(Response.Status.CREATED).entity(scheduleService.create(request)).build();
    }

    @PUT
    @Path("/{id}")
    public ScheduleBlockResponse update(@PathParam("id") String id, @Valid ScheduleBlockRequest request) {
        return scheduleService.update(id, request);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id) {
        scheduleService.delete(id);
        return Response.noContent().build();
    }
}
