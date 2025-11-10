// src/main/java/com/team103/repository/CourseRepository.java
package com.team103.repository;

import com.team103.model.Course;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends MongoRepository<Course, String> {

    /** ✅ 교사별 조회 */
    List<Course> findByTeacherId(String teacherId);

    /** ✅ Students 배열이 문자열 또는 객체 배열 두 형태 모두 지원 */
    @Query(value = "{ $or: [ { 'Students': ?0 }, { 'Students': { $elemMatch: { 'Student_ID': ?0 } } } ] }")
    List<Course> findByStudentsContaining(String studentId);

    /** ✅ Room + Academy 동시 매칭 */
    List<Course> findByRoomNumberAndAcademyNumber(Integer roomNumber, Integer academyNumber);

    /** ✅ Class_ID 기반 조회 (Mongo 필드명 명시) */
    @Query("{ 'Class_ID': ?0 }")
    Optional<Course> findByClassId(String classId);

    /** ✅ 학원번호 기반 전체 코스 조회 (원장 종합뷰용) */
    @Query("{ 'Academy_Number': ?0 }")
    List<Course> findByAcademyNumber(int academyNumber);

    /** ✅ 단일 방 번호 */
    List<Course> findByRoomNumber(Integer roomNumber);

    /** ✅ 복수 Room_Numbers 배열 내 포함 검색 */
    List<Course> findByRoomNumbersContaining(Integer roomNumber);

    /** ✅ Null-safe 헬퍼 */
    default Course getByClassIdOrNull(String classId) {
        return findByClassId(classId).orElse(null);
    }
}
