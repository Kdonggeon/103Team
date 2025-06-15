package com.team103.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.team103.model.Attendance;

public interface AttendanceRepository extends MongoRepository<Attendance, String> {
//    List<Attendance> findByClassIdAndAttendedStudentsContaining(String classId, String studentId);
    
    List<Attendance> findByClassIdAndDate(String classId, String date);
    
//    List<Attendance> findByStudentId(String studentId);
    
    @Query("{ 'Attendance_List': { $elemMatch: { 'Student_ID': ?0 } } }")
    List<Attendance> findByStudentInAttendanceList(String studentId);

    
    



}

