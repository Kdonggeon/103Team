package com.team103.repository;

import com.team103.model.Student;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface StudentRepository extends MongoRepository<Student, String> {

//    @Query("{ 'Student_ID': ?0, 'Student_PW': ?1 }")
//    Student findByStudentIdAndStudentPw(String studentId, String studentPw);

    Student findByStudentId(String studentId);

    boolean existsByStudentId(String studentId);

    List<Student> findByParentsNumber(String parentsNumber);

    List<Student> findByStudentIdIn(List<String> studentIds);
    
    
    // 🔥 명시적 쿼리로 수정
    @Query("{ 'Student_Name': ?0, 'Student_Phone_Number': ?1 }")
    Student findByStudentNameAndStudentPhoneNumber(String name, String phoneNumber);


}
