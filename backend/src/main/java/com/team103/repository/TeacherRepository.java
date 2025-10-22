package com.team103.repository;

import com.team103.model.Teacher;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface TeacherRepository extends MongoRepository<Teacher, String> {
    boolean existsByTeacherId(String teacherId);
    Teacher findByTeacherId(String teacherId);
    @Query("{ 'academyNumbers': ?0 }")
    List<Teacher> findByAcademyNumber(int academyNumber);
    Teacher findByTeacherNameAndTeacherPhoneNumber(String name, String phone);
    List<Teacher> findByTeacherIdIn(List<String> ids);
    

}
