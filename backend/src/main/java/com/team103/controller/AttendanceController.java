package com.team103.controller;

import com.team103.dto.AttendanceResponse;
import com.team103.model.Academy;
import com.team103.model.Attendance;
import com.team103.model.AttendanceEntry;
import com.team103.model.Course;
import com.team103.model.Student;
import com.team103.model.Teacher;
import com.team103.repository.AcademyRepository;
import com.team103.repository.AttendanceRepository;
import com.team103.repository.CourseRepository;
import com.team103.repository.StudentRepository;
import com.team103.repository.TeacherRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/students")
public class AttendanceController {

    @Autowired private AttendanceRepository attendanceRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private AcademyRepository academyRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private TeacherRepository teacherRepository;

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 학생 본인의 출석 기록(수업명/학원명/날짜/상태) – 날짜 오름차순(과거→최근) */
    @GetMapping("/{studentId}/attendance")
    public ResponseEntity<List<AttendanceResponse>> getAttendanceForStudent(@PathVariable String studentId) {

        // 1) 학생이 포함된 출석 문서 조회
        List<Attendance> attends = attendanceRepository.findByStudentInAttendanceList(studentId);

        // 2) 학원명 계산(학생 → academyNumbers → Academy)
        String academyNameForStudent = resolveAcademyNameByStudent(studentId);

        // 3) 변환: Attendance -> AttendanceResponse
        List<AttendanceResponse> out = new ArrayList<>();
        for (Attendance att : attends) {
            String status = resolveStatus(att, studentId);

            Course course = courseRepository.findByClassId(att.getClassId());
            String className = (course != null && course.getClassName() != null) ? course.getClassName() : "";

            // 코스/교사를 통한 폴백도 준비
            String academyName = academyNameForStudent;
            if (academyName.isEmpty()) {
                academyName = resolveAcademyNameByCourseTeacher(course);
            }

            AttendanceResponse dto = new AttendanceResponse();
            dto.setClassName(className);
            dto.setAcademyName(academyName);
            dto.setDate(att.getDate());     // "yyyy-MM-dd"
            dto.setStatus(status);

            out.add(dto);
        }

        // 4) 날짜 오름차순 정렬
        out.sort(Comparator.comparing(a -> LocalDate.parse(a.getDate(), DF)));
        return new ResponseEntity<>(out, HttpStatus.OK);
    }

    /** 출석 문서에서 해당 학생의 상태 찾기 */
    private String resolveStatus(Attendance att, String studentId) {
        if (att == null || att.getAttendanceList() == null) return "정보 없음";
        for (AttendanceEntry e : att.getAttendanceList()) {
            if (e != null && studentId.equals(e.getStudentId())) {
                return (e.getStatus() != null) ? e.getStatus() : "정보 없음";
            }
        }
        return "정보 없음";
    }

    /** 학생 → academyNumbers 첫 번째 번호 → Academy.name */
    private String resolveAcademyNameByStudent(String studentId) {
        Student stu = studentRepository.findByStudentId(studentId);
        if (stu == null || stu.getAcademyNumbers() == null || stu.getAcademyNumbers().isEmpty()) return "";
        Integer no = stu.getAcademyNumbers().get(0);
        Academy a = academyRepository.findByAcademyNumber(no);
        return (a != null && a.getName() != null) ? a.getName() : "";
    }

    /** 코스의 교사 → academyNumbers 첫 번째 번호 → Academy.name (폴백용) */
    private String resolveAcademyNameByCourseTeacher(Course course) {
        if (course == null || course.getTeacherId() == null) return "";
        Teacher t = teacherRepository.findByTeacherId(course.getTeacherId());
        if (t == null || t.getAcademyNumbers() == null || t.getAcademyNumbers().isEmpty()) return "";
        Integer no = t.getAcademyNumbers().get(0);
        Academy a = academyRepository.findByAcademyNumber(no);
        return (a != null && a.getName() != null) ? a.getName() : "";
    }
}
