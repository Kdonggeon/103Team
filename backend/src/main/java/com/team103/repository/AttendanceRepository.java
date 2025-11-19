// src/main/java/com/team103/repository/AttendanceRepository.java
package com.team103.repository;

import com.team103.model.Attendance;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Collection;
import java.util.List;

public interface AttendanceRepository extends MongoRepository<Attendance, String> {

    List<Attendance> findByClassId(String classId);

    List<Attendance> findByClassIdAndDate(String classId, String date);

    Attendance findFirstByClassIdAndDate(String classId, String date);

    @Query(value = "{ 'Attendance_List.Student_ID': ?0 }")
    List<Attendance> findByStudentInAttendanceList(String studentId);

    List<Attendance> findByClassIdInAndDateBetween(Collection<String> classIds, String from, String to);

    @Query(value = "{ 'Type': ?0, 'Date': ?1 }")
    List<Attendance> findByTypeAndDate(String type, String date);

    // 🔥 새로 추가 (A안 핵심)
    @Query(value = "{ 'Type': ?0, 'Date': ?1, 'Academy_Number': ?2 }")
    List<Attendance> findByTypeAndDateAndAcademyNumber(String type, String date, Integer academyNumber);
}
