package com.studytracker.service;

import jakarta.ws.rs.BadRequestException;
import org.bson.types.ObjectId;

public final class Ids {

    private Ids() {}

    public static ObjectId parse(String id) {
        if (id == null || !ObjectId.isValid(id)) {
            throw new BadRequestException("El id '" + id + "' no es válido");
        }
        return new ObjectId(id);
    }
}
