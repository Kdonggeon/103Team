// src/main/java/com/team103/repository/AttendanceRepository.java
package com.team103.repository;

import com.team103.model.Attendance;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Collection;
import java.util.List;

/**
 * attendances 컬렉션용 리포지토리
 *
 * 컬렉션 구조:
 * 1) 수업 출석 문서
 *    - Class_ID, Date, Attendance_List[], Seat_Assignments[]
 * 2) entrance 입구 출석 문서
 *    - Type = "entrance", Date, Attendance_List[]
 *
 * 👉 학생 출석 조회용 메서드는 "수업 출석 문서(Class_ID 존재)"만 보도록 필터링 필요
 */
public interface AttendanceRepository extends MongoRepository<Attendance, String> {

    /** 특정 수업(classId)의 모든 출석 문서 */
    List<Attendance> findByClassId(String classId);

    /** 특정 수업 + 날짜 단건/복수 조회 */
    List<Attendance> findByClassIdAndDate(String classId, String date);

    /** 특정 수업 + 날짜 첫 번째 문서 (하루 1문서 전략에서 주로 사용) */
    Attendance findFirstByClassIdAndDate(String classId, String date);

    /**
     * 학생 ID가 Attendance_List 안에 포함된 출석 문서 조회
     *
     * ⚠️ entrance 문서까지 같이 나오는 것을 막기 위해
     *    - Attendance_List.Student_ID == studentId
     *    - AND Class_ID 필드가 존재하는 문서만
     *    → "수업 출석" 문서만 대상
     */
    @Query(value = "{ 'Attendance_List.Student_ID': ?0, 'Class_ID': { $exists: true } }")
    List<Attendance> findByStudentInAttendanceList(String studentId);

    /**
     * 여러 수업(classIds)에 대해 날짜 범위(from ~ to) 출석 문서 조회
     * - 주로 통계/기간 조회용
     */
    List<Attendance> findByClassIdInAndDateBetween(Collection<String> classIds, String from, String to);

    /**
     * Type + Date 기반 조회
     * - entrance 등 특수 타입 문서 조회용
     * - 예: Type = "entrance", Date = "2025-11-19"
     */
    @Query(value = "{ 'Type': ?0, 'Date': ?1 }")
    List<Attendance> findByTypeAndDate(String type, String date);

    /**
     * Type + Date + Academy_Number 기반 조회
     * - A안(학원별 entrance 문서 분리) 시나리오에서 사용
     * - 아직 entrance 문서에 Academy_Number 안 넣었으면 결과는 항상 빈 리스트가 됨
     *
     * 👉 지금 바로 쓰고 싶으면 EntranceCheckInController 에서
     *    attendances 문서에도 Academy_Number 필드를 같이 set 해줘야 함.
     */
    @Query(value = "{ 'Type': ?0, 'Date': ?1, 'Academy_Number': ?2 }")
    List<Attendance> findByTypeAndDateAndAcademyNumber(String type, String date, Integer academyNumber);
}
