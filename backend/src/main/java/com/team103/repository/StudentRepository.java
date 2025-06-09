package com.team103.repository;

import com.team103.model.Student;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface StudentRepository extends MongoRepository<Student, String> {

    // 🔄 명시적 쿼리: String 타입으로 수정
    @Query("{ 'Student_ID': ?0, 'Student_PW': ?1 }")
    Student findByStudentIdAndStudentPw(String studentId, String studentPw);

    // 🔄 기본 검색: studentId 기준
    Student findByStudentId(String studentId);

    // 필요 시 사용
    boolean existsByStudentId(String studentId);
    
    Student findByStudentNameAndStudentPhoneNumber(String name, String phoneNumber);
}
