package com.studytracker.resource;

import com.studytracker.dto.ManualSessionRequest;
import com.studytracker.dto.SessionResponse;
import com.studytracker.dto.SessionResultResponse;
import com.studytracker.dto.StartSessionRequest;
import com.studytracker.dto.StopSessionRequest;
import com.studytracker.service.SessionService;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.LocalDate;
import java.util.List;

@Path("/sessions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Sesiones y timer")
public class SessionResource {

    private final SessionService sessionService;

    public SessionResource(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GET
    @Operation(summary = "Historial de sesiones terminadas (fechas en formato yyyy-MM-dd)")
    public List<SessionResponse> list(@QueryParam("from") LocalDate from,
                                      @QueryParam("to") LocalDate to,
                                      @QueryParam("subjectId") String subjectId) {
        return sessionService.list(from, to, subjectId);
    }

    @GET
    @Path("/active")
    @Operation(summary = "La sesión en curso, o 204 si no hay ninguna")
    public Response active() {
        return sessionService.active()
                .map(s -> Response.ok(s).build())
                .orElseGet(() -> Response.noContent().build());
    }

    @POST
    @Path("/start")
    public Response start(@Valid StartSessionRequest request) {
        return Response.status(Response.Status.CREATED).entity(sessionService.start(request)).build();
    }

    @POST
    @Path("/{id}/pause")
    public SessionResponse pause(@PathParam("id") String id) {
        return sessionService.pause(id);
    }

    @POST
    @Path("/{id}/resume")
    public SessionResponse resume(@PathParam("id") String id) {
        return sessionService.resume(id);
    }

    @POST
    @Path("/{id}/pomodoro")
    @Operation(summary = "Marca un pomodoro como completado y empieza el descanso")
    public SessionResponse completePomodoro(@PathParam("id") String id) {
        return sessionService.completePomodoro(id);
    }

    @POST
    @Path("/{id}/stop")
    public SessionResultResponse stop(@PathParam("id") String id, @Valid StopSessionRequest request) {
        return sessionService.stop(id, request);
    }

    @POST
    @Operation(summary = "Registrar a mano una sesión ya terminada")
    public Response createManual(@Valid ManualSessionRequest request) {
        return Response.status(Response.Status.CREATED).entity(sessionService.createManual(request)).build();
    }

    @PUT
    @Path("/{id}")
    public SessionResultResponse update(@PathParam("id") String id, @Valid ManualSessionRequest request) {
        return sessionService.update(id, request);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id) {
        sessionService.delete(id);
        return Response.noContent().build();
    }
}
