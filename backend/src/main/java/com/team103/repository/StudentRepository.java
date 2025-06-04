package com.team103.repository;

import com.team103.model.Student;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface StudentRepository extends MongoRepository<Student, String> {

    // 명시적 쿼리 작성
    @Query("{ 'Student_ID': ?0, 'Student_PW': ?1 }")
    Student findByStudentIdAndStudentPw(long studentId, int studentPw);

    Student findByStudentId(long studentId);
}
