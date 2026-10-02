package com.studytracker.exception;

import com.studytracker.dto.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import java.util.List;

/** Convierte las excepciones en respuestas JSON uniformes: { status, message, errors }. */
public class ErrorMappers {

    private static final Logger LOG = Logger.getLogger(ErrorMappers.class);

    @ServerExceptionMapper
    public Response validation(ConstraintViolationException e) {
        List<ErrorResponse.FieldError> errors = e.getConstraintViolations().stream()
                .map(v -> new ErrorResponse.FieldError(fieldName(v), v.getMessage()))
                .toList();
        String message = errors.isEmpty() ? "Datos inválidos" : errors.get(0).message();
        return json(400, new ErrorResponse(400, message, errors));
    }

    @ServerExceptionMapper
    public Response webApplication(WebApplicationException e) {
        int status = e.getResponse().getStatus();
        // Los mensajes por defecto de JAX-RS ("HTTP 400 Bad Request") se cambian por uno en español
        String message = e.getMessage() == null || e.getMessage().startsWith("HTTP ")
                ? defaultMessage(status)
                : e.getMessage();
        return json(status, ErrorResponse.of(status, message));
    }

    private static String defaultMessage(int status) {
        return switch (status) {
            case 400 -> "La solicitud no es válida (revisa el formato de los datos)";
            case 404 -> "No se encontró el recurso";
            case 405 -> "Método no permitido";
            case 415 -> "El contenido debe enviarse como JSON";
            default -> "Error " + status;
        };
    }

    @ServerExceptionMapper
    public Response unexpected(Exception e) {
        LOG.error("Error inesperado", e);
        return json(500, ErrorResponse.of(500, "Ocurrió un error inesperado en el servidor"));
    }

    private static Response json(int status, ErrorResponse body) {
        return Response.status(status).type(MediaType.APPLICATION_JSON).entity(body).build();
    }

    /** "create.request.name" → "name" */
    private static String fieldName(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        return path.substring(path.lastIndexOf('.') + 1);
    }
}
