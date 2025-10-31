package com.team103.repository;

import com.team103.model.Attendance;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Collection;
import java.util.List;

public interface AttendanceRepository extends MongoRepository<Attendance, String> {

    // ✅ 수업 ID 전체(날짜 제한 없음) — ClassAttendanceController에서 사용
    List<Attendance> findByClassId(String classId);

    // ✅ 특정 날짜 목록 — TeacherController 등에서 사용
    List<Attendance> findByClassIdAndDate(String classId, String date);

    // ✅ 한 건만 필요할 때
    Attendance findFirstByClassIdAndDate(String classId, String date);

    // ✅ 출석 리스트 내부(Student_ID) 포함 여부로 검색 — 학생/학부모 조회에서 사용
    @Query(value = "{ 'Attendance_List.Student_ID': ?0 }")
    List<Attendance> findByStudentInAttendanceList(String studentId);
    
    // 특정 수업들 + 날짜 구간(양끝 포함) — 월/주 캘린더에서 사용
    List<Attendance> findByClassIdInAndDateBetween(Collection<String> classIds, String from, String to);


}
