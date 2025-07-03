package com.team103.repository;

import com.team103.model.Teacher;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TeacherRepository extends MongoRepository<Teacher, String> {
    boolean existsByTeacherId(String teacherId);
    Teacher findByTeacherId(String teacherId);
    
    Teacher findByTeacherNameAndTeacherPhoneNumber(String name, String phone);

}
