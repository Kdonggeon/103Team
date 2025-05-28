package com.team103.repository;

import com.team103.model.Student;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface StudentRepository extends MongoRepository<Student, String> {
    Student findByStudentId(long studentId); // studentId로 조회하는 메서드
}
