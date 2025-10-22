package com.team103.repository;

import com.team103.model.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface StudentRepository extends MongoRepository<Student, String> {

    // ---- 기존 메서드 유지 ----
    Student findByStudentId(String studentId);

    boolean existsByStudentId(String studentId);

    List<Student> findByParentsNumber(String parentsNumber);

    List<Student> findByStudentIdIn(List<String> studentIds);

    // 이름 + 전화번호 정확일치 (아이디 찾기용 등)
    @Query(value = "{ 'Student_Name': ?0, 'Student_Phone_Number': ?1 }")
    Student findByStudentNameAndStudentPhoneNumber(String name, String phoneNumber);


    // ---- ⬇️ 추가: 학원 + 이름 부분일치(대소문자 무시) ----
    // Academy_Numbers 배열에 포함되는 학생 중, 이름에 키워드가 포함된 케이스
    @Query(value = "{ 'Academy_Numbers': ?0, 'Student_Name': { $regex: ?1, $options: 'i' } }")
    List<Student> findByAcademyAndNameLike(Integer academyNumber, String nameLike);

    // 학년까지 함께 필터링 (grade가 있을 때)
    @Query(value = "{ 'Academy_Numbers': ?0, 'Grade': ?1, 'Student_Name': { $regex: ?2, $options: 'i' } }")
    List<Student> findByAcademyAndGradeAndNameLike(Integer academyNumber, Integer grade, String nameLike);


    // ---- (선택) 페이징 지원 버전 ----
    @Query(value = "{ 'Academy_Numbers': ?0, 'Student_Name': { $regex: ?1, $options: 'i' } }")
    Page<Student> pageByAcademyAndNameLike(Integer academyNumber, String nameLike, Pageable pageable);

    @Query(value = "{ 'Academy_Numbers': ?0, 'Grade': ?1, 'Student_Name': { $regex: ?2, $options: 'i' } }")
    Page<Student> pageByAcademyAndGradeAndNameLike(Integer academyNumber, Integer grade, String nameLike, Pageable pageable);
}
