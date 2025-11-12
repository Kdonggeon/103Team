package com.team103.repository;

import com.team103.model.Student;
<<<<<<< HEAD
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface StudentRepository extends MongoRepository<Student, String> {

    // --- 기본 ---
=======
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.util.List;
import java.util.Optional;

public interface StudentRepository extends MongoRepository<Student, String> {

>>>>>>> new2
    Student findByStudentId(String studentId);
    boolean existsByStudentId(String studentId);
    List<Student> findByParentsNumber(String parentsNumber);
    List<Student> findByStudentIdIn(List<String> studentIds);
<<<<<<< HEAD
    void deleteByStudentId(String studentId);

    // 이름 + 전화번호 (대소문자/표기 혼합 대응)
    @Query(value =
        "{ $and: [" +
        "  { $or: [ { 'Student_Name': ?0 }, { 'studentName': ?0 }, { 'name': ?0 } ] }," +
        "  { $or: [ { 'Student_Phone_Number': ?1 }, { 'studentPhoneNumber': ?1 }, { 'phone': ?1 } ] }" +
        "] }")
    Student findByStudentNameAndStudentPhoneNumber(String name, String phoneNumber);

    // ==========================
    // ✅ 학원 + 이름(부분일치, 대소문자무시)
    // - 학원: Academy_Numbers / academyNumbers (배열), Academy_Number / academyNumber (단일)
    // - 타입: 숫자/문자 모두 대응(academyNumber, academyNumberStr)
    // - 이름: Student_Name / studentName / name
    // ==========================
    @Query(value =
        "{ $and: [" +
        "  { $or: [" +
        "      { 'academyNumbers': ?0 }, { 'academyNumber': ?0 }, { 'Academy_Numbers': ?0 }, { 'Academy_Number': ?0 }," +
        "      { 'academyNumbers': ?1 }, { 'academyNumber': ?1 }, { 'Academy_Numbers': ?1 }, { 'Academy_Number': ?1 }" +
        "  ]}," +
        "  { $or: [" +
        "      { 'studentName':  { $regex: ?2, $options: 'i' } }," +
        "      { 'Student_Name': { $regex: ?2, $options: 'i' } }," +
        "      { 'name':         { $regex: ?2, $options: 'i' } }" +
        "  ]}" +
        "] }")
    List<Student> findByAcademyLooseAndNameLike(Integer academyNumber, String academyNumberStr, String nameLike);

    @Query(value =
        "{ $and: [" +
        "  { $or: [" +
        "      { 'academyNumbers': ?0 }, { 'academyNumber': ?0 }, { 'Academy_Numbers': ?0 }, { 'Academy_Number': ?0 }," +
        "      { 'academyNumbers': ?1 }, { 'academyNumber': ?1 }, { 'Academy_Numbers': ?1 }, { 'Academy_Number': ?1 }" +
        "  ]}," +
        "  { $or: [ { 'Grade': ?2 }, { 'grade': ?2 } ] }," +
        "  { $or: [" +
        "      { 'studentName':  { $regex: ?3, $options: 'i' } }," +
        "      { 'Student_Name': { $regex: ?3, $options: 'i' } }," +
        "      { 'name':         { $regex: ?3, $options: 'i' } }" +
        "  ]}" +
        "] }")
    List<Student> findByAcademyLooseAndGradeAndNameLike(Integer academyNumber, String academyNumberStr, Integer grade, String nameLike);

    // 전역 이름 검색(폴백) — 표기 혼합 지원
    @Query(value =
        "{ $or: [" +
        "  { 'studentName':  { $regex: ?0, $options: 'i' } }," +
        "  { 'Student_Name': { $regex: ?0, $options: 'i' } }," +
        "  { 'name':         { $regex: ?0, $options: 'i' } }" +
        "] }")
    List<Student> findByNameLikeAny(String nameLike);

    @Query(value =
        "{ $and: [" +
        "  { $or: [" +
        "    { 'studentName':  { $regex: ?0, $options: 'i' } }," +
        "    { 'Student_Name': { $regex: ?0, $options: 'i' } }," +
        "    { 'name':         { $regex: ?0, $options: 'i' } }" +
        "  ]}," +
        "  { $or: [ { 'Grade': ?1 }, { 'grade': ?1 } ] }" +
        "] }")
    List<Student> findByNameLikeAnyAndGrade(String nameLike, Integer grade);

    // (옵션) 페이징 버전들
    @Query(value =
        "{ $and: [" +
        "  { $or: [" +
        "      { 'academyNumbers': ?0 }, { 'academyNumber': ?0 }, { 'Academy_Numbers': ?0 }, { 'Academy_Number': ?0 }," +
        "      { 'academyNumbers': ?1 }, { 'academyNumber': ?1 }, { 'Academy_Numbers': ?1 }, { 'Academy_Number': ?1 }" +
        "  ]}," +
        "  { $or: [" +
        "      { 'studentName':  { $regex: ?2, $options: 'i' } }," +
        "      { 'Student_Name': { $regex: ?2, $options: 'i' } }," +
        "      { 'name':         { $regex: ?2, $options: 'i' } }" +
        "  ]}" +
        "] }")
    Page<Student> pageByAcademyLooseAndNameLike(Integer academyNumber, String academyNumberStr, String nameLike, Pageable pageable);

    @Query(value =
        "{ $and: [" +
        "  { $or: [" +
        "      { 'academyNumbers': ?0 }, { 'academyNumber': ?0 }, { 'Academy_Numbers': ?0 }, { 'Academy_Number': ?0 }," +
        "      { 'academyNumbers': ?1 }, { 'academyNumber': ?1 }, { 'Academy_Numbers': ?1 }, { 'Academy_Number': ?1 }" +
        "  ]}," +
        "  { $or: [ { 'Grade': ?2 }, { 'grade': ?2 } ] }," +
        "  { $or: [" +
        "      { 'studentName':  { $regex: ?3, $options: 'i' } }," +
        "      { 'Student_Name': { $regex: ?3, $options: 'i' } }," +
        "      { 'name':         { $regex: ?3, $options: 'i' } }" +
        "  ]}" +
        "] }")
    Page<Student> pageByAcademyLooseAndGradeAndNameLike(Integer academyNumber, String academyNumberStr, Integer grade, String nameLike, Pageable pageable);

    /** ✅ HEAD 브랜치 추가된 쿼리: 정확 일치 필터 (academy + grade + nameLike) */
    @Query(value = "{ 'Academy_Numbers': ?0, 'Grade': ?1, 'Student_Name': { $regex: ?2, $options: 'i' } }")
    Page<Student> pageByAcademyAndGradeAndNameLike(Integer academyNumber, Integer grade, String nameLike, Pageable pageable);
=======

    @Query("{ 'Student_Name': ?0, 'Student_Phone_Number': ?1 }")
    Student findByStudentNameAndStudentPhoneNumber(String name, String phoneNumber);

    // ✅ 추가: 학원 번호로 학생 목록 조회
    @Query("{ 'Academy_Numbers': ?0 }")
    List<Student> findByAcademyNumber(int academyNumber);
>>>>>>> new2
}
