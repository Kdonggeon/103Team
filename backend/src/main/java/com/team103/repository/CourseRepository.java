// backend/src/main/java/com/team103/repository/CourseRepository.java
package com.team103.repository;

import com.team103.model.Course;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends MongoRepository<Course, String> {

    // 교사별 조회 (Course에 @Field("Teacher_ID")로 매핑돼 있다고 가정)
    List<Course> findByTeacherId(String teacherId);

    // ✅ 핵심 수정: 파생쿼리 → 명시 쿼리
    // Students 배열이 ["12345", ...] 또는 [{ Student_ID: "12345", ... }, ...] 두 형태 모두 지원
    @Query(value = "{ $or: [ { 'Students': ?0 }, { 'Students': { $elemMatch: { 'Student_ID': ?0 } } } ] }")
    List<Course> findByStudentsContaining(String studentId);

    List<Course> findByRoomNumberAndAcademyNumber(Integer roomNumber, Integer academyNumber);

    /** DB 실제 필드가 Class_ID 이므로 명시 쿼리 */
    @Query("{ 'Class_ID': ?0 }")
    Optional<Course> findByClassId(String classId);

    /** 같은 방 번호의 코스 목록 */
    List<Course> findByRoomNumber(Integer roomNumber);

    /** 편의 메서드 */
    default Course getByClassIdOrNull(String classId) {
        return findByClassId(classId).orElse(null);
    }
}
