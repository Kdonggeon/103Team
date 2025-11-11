package com.team103.repository;

import com.team103.model.Student;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.util.List;
import java.util.Optional;

public interface StudentRepository extends MongoRepository<Student, String> {

    Student findByStudentId(String studentId);

    boolean existsByStudentId(String studentId);

    List<Student> findByParentsNumber(String parentsNumber);

    List<Student> findByStudentIdIn(List<String> studentIds);

    @Query("{ 'Student_Name': ?0, 'Student_Phone_Number': ?1 }")
    Student findByStudentNameAndStudentPhoneNumber(String name, String phoneNumber);

    // ✅ 추가: 학원 번호로 학생 목록 조회
    @Query("{ 'Academy_Numbers': ?0 }")
    List<Student> findByAcademyNumber(int academyNumber);
}
