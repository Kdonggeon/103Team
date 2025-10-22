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
@RequestMapping("/api/parents")
public class ParentAttendanceController {

    private final AttendanceRepository attendanceRepository;
    private final CourseRepository courseRepository;
    private final AcademyRepository academyRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;

    public ParentAttendanceController(AttendanceRepository attendanceRepository,
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

    /** (주의) Path는 parentId가 아니라 '자녀 studentId' – 날짜 오름차순(과거→최근) */
    @GetMapping("/{studentId}/attendance")
    public ResponseEntity<List<AttendanceResponse>> getAttendanceForChild(@PathVariable String studentId) {

        // 1) 자녀가 포함된 출석 문서 조회
        List<Attendance> attends = attendanceRepository.findByStudentInAttendanceList(studentId);

        // 2) 자녀 기준 학원명
        String academyNameForStudent = resolveAcademyNameByStudent(studentId);

        // 3) 변환
        List<AttendanceResponse> out = new ArrayList<>();
        for (Attendance att : attends) {
            String status = resolveStatus(att, studentId);

            // ✅ Optional 대응
            Course course = courseRepository.findByClassId(att.getClassId()).orElse(null);
            // 또는: Course course = courseRepository.getByClassIdOrNull(att.getClassId());

            String className = (course != null && course.getClassName() != null) ? course.getClassName() : "";

            String academyName = academyNameForStudent;
            if (academyName.isEmpty()) {
                academyName = resolveAcademyNameByCourseTeacher(course);
            }

            AttendanceResponse dto = new AttendanceResponse();
            dto.setClassName(className);
            dto.setAcademyName(academyName);
            dto.setDate(att.getDate());
            dto.setStatus(status);

            out.add(dto);
        }

        out.sort(Comparator.comparing(a -> LocalDate.parse(a.getDate(), DF)));
        return new ResponseEntity<>(out, HttpStatus.OK);
    }

    /** 출석 문서에서 해당 학생의 상태 찾기 — Map/POJO 혼재까지 안전 */
    private String resolveStatus(Attendance att, String studentId) {
        if (att == null || att.getAttendanceList() == null) return "정보 없음";
        for (Object e : att.getAttendanceList()) {
            if (e == null) continue;

            if (e instanceof Attendance.Item item) {
                if (studentId.equals(item.getStudentId())) {
                    return (item.getStatus() != null) ? item.getStatus() : "정보 없음";
                }
                continue;
            }

            if (e instanceof Map<?, ?> m) {
                Object sid = m.get("Student_ID");
                if (sid != null && studentId.equals(String.valueOf(sid))) {
                    Object st = m.get("Status");
                    return (st != null) ? String.valueOf(st) : "정보 없음";
                }
                continue;
            }

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

    // ===== 공통 유틸 (학생/교사 경로) =====

    private String resolveAcademyNameByStudent(String studentId) {
        Student stu = studentRepository.findByStudentId(studentId);
        if (stu == null || stu.getAcademyNumbers() == null || stu.getAcademyNumbers().isEmpty()) return "";
        Integer no = stu.getAcademyNumbers().get(0);
        Academy a = academyRepository.findByAcademyNumber(no);
        return (a != null && a.getName() != null) ? a.getName() : "";
    }

    private String resolveAcademyNameByCourseTeacher(Course course) {
        if (course == null || course.getTeacherId() == null) return "";
        Teacher t = teacherRepository.findByTeacherId(course.getTeacherId());
        if (t == null || t.getAcademyNumbers() == null || t.getAcademyNumbers().isEmpty()) return "";
        Integer no = t.getAcademyNumbers().get(0);
        Academy a = academyRepository.findByAcademyNumber(no);
        return (a != null && a.getName() != null) ? a.getName() : "";
    }
}
