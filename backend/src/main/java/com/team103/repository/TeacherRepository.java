package com.team103.repository;

import com.team103.model.Teacher;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TeacherRepository extends MongoRepository<Teacher, String> {
    boolean existsByUsername(String username);
    Teacher findByUsername(String username);
}
