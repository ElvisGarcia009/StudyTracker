package com.studytracker.repository;

import com.studytracker.entity.Subject;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SubjectRepository implements PanacheMongoRepository<Subject> {}
