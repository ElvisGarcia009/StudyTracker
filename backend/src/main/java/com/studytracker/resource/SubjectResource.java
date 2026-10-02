package com.studytracker.resource;

import com.studytracker.dto.SubjectRequest;
import com.studytracker.dto.SubjectResponse;
import com.studytracker.service.SubjectService;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/subjects")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Materias")
public class SubjectResource {

    private final SubjectService subjectService;

    public SubjectResource(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @GET
    public List<SubjectResponse> list() {
        return subjectService.list();
    }

    @POST
    public Response create(@Valid SubjectRequest request) {
        return Response.status(Response.Status.CREATED).entity(subjectService.create(request)).build();
    }

    @PUT
    @Path("/{id}")
    public SubjectResponse update(@PathParam("id") String id, @Valid SubjectRequest request) {
        return subjectService.update(id, request);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id) {
        subjectService.delete(id);
        return Response.noContent().build();
    }
}
