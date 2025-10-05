package com.team103.controller;

import com.team103.dto.AttendanceResponse;
import com.team103.model.Academy;
import com.team103.model.Attendance;
import com.team103.model.Course;
import com.team103.model.Student;
import com.team103.model.Teacher;
import com.team103.repository.AcademyRepository;
import com.team103.repository.AttendanceRepository;
import com.team103.repository.CourseRepository;
import com.team103.repository.StudentRepository;
import com.team103.repository.TeacherRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/students")
public class AttendanceController {

    private final AttendanceRepository attendanceRepository;
    private final CourseRepository courseRepository;
    private final AcademyRepository academyRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;

    public AttendanceController(AttendanceRepository attendanceRepository,
                                CourseRepository courseRepository,
                                AcademyRepository academyRepository,
                                StudentRepository studentRepository,
                                TeacherRepository teacherRepository) {
        this.attendanceRepository = attendanceRepository;
        this.courseRepository = courseRepository;
        this.academyRepository = academyRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
    }

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 학생 본인의 출석 기록(수업명/학원명/날짜/상태/시간) – 날짜 오름차순 */
    @GetMapping("/{studentId}/attendance")
    public ResponseEntity<List<AttendanceResponse>> getAttendanceForStudent(@PathVariable String studentId) {

        // 1) 학생이 포함된 출석 문서 조회
        List<Attendance> attends = attendanceRepository.findByStudentInAttendanceList(studentId);

        // 2) 학원명 계산(학생 → academyNumbers → Academy)
        String academyNameForStudent = resolveAcademyNameByStudent(studentId);

        // 3) 변환
        List<AttendanceResponse> out = new ArrayList<>();
        for (Attendance att : attends) {
            String status = resolveStatus(att, studentId);

            Course course = courseRepository.findByClassId(att.getClassId());
            String className = (course != null && course.getClassName() != null) ? course.getClassName() : "";

            String academyName = academyNameForStudent;
            if (academyName.isEmpty()) {
                academyName = resolveAcademyNameByCourseTeacher(course);
            }

            AttendanceResponse dto = new AttendanceResponse();
            dto.setClassName(className);
            dto.setAcademyName(academyName);
            dto.setDate(att.getDate()); // "yyyy-MM-dd"
            dto.setStatus(status);

            if (course != null) {
                dto.setStartTime(formatTime(course.getStartTime())); // "HH:mm"
                dto.setEndTime(formatTime(course.getEndTime()));     // "HH:mm"
            } else {
                dto.setStartTime(null);
                dto.setEndTime(null);
            }

            out.add(dto);
        }

        // 4) 날짜 오름차순 정렬
        out.sort(Comparator.comparing(a -> LocalDate.parse(a.getDate(), DF)));
        return new ResponseEntity<>(out, HttpStatus.OK);
    }

    /** 출석 문서에서 해당 학생의 상태 찾기 — Map/POJO 혼재까지 안전 */
    private String resolveStatus(Attendance att, String studentId) {
        if (att == null || att.getAttendanceList() == null) return "정보 없음";
        for (Object e : att.getAttendanceList()) {
            if (e == null) continue;

            // 1) 우리가 정의한 내부 클래스 Item
            if (e instanceof Attendance.Item item) {
                if (studentId.equals(item.getStudentId())) {
                    return (item.getStatus() != null) ? item.getStatus() : "정보 없음";
                }
                continue;
            }

            // 2) Map 형태(직접 upsert 등으로 들어온 경우)
            if (e instanceof Map<?, ?> m) {
                Object sid = m.get("Student_ID");
                if (sid != null && studentId.equals(String.valueOf(sid))) {
                    Object st = m.get("Status");
                    return (st != null) ? String.valueOf(st) : "정보 없음";
                }
                continue;
            }

            // 3) 다른 POJO (getStudentId/getStatus 리플렉션)
            try {
                Object sid = e.getClass().getMethod("getStudentId").invoke(e);
                if (sid != null && studentId.equals(String.valueOf(sid))) {
                    Object st = e.getClass().getMethod("getStatus").invoke(e);
                    return (st != null) ? String.valueOf(st) : "정보 없음";
                }
            } catch (Exception ignore) {}
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

    /** 코스의 교사 → academyNumbers 첫 번째 → Academy.name (폴백) */
    private String resolveAcademyNameByCourseTeacher(Course course) {
        if (course == null || course.getTeacherId() == null) return "";
        Teacher t = teacherRepository.findByTeacherId(course.getTeacherId());
        if (t == null || t.getAcademyNumbers() == null || t.getAcademyNumbers().isEmpty()) return "";
        Integer no = t.getAcademyNumbers().get(0);
        Academy a = academyRepository.findByAcademyNumber(no);
        return (a != null && a.getName() != null) ? a.getName() : "";
    }

    /** "H:mm", "HH:mm", "9:0" 등 → "HH:mm" */
    private String formatTime(String t) {
        if (t == null || t.trim().isEmpty()) return null;
        try {
            String[] p = t.split(":");
            int hh = Integer.parseInt(p[0].trim());
            int mm = (p.length > 1) ? Integer.parseInt(p[1].trim()) : 0;
            return String.format(java.util.Locale.KOREA, "%02d:%02d", hh, mm);
        } catch (Exception e) {
            return t;
        }
    }
}
