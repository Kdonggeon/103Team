package com.team103.controller;

import com.team103.model.Attendance;
import com.team103.model.Course;
import com.team103.model.Student;
import com.team103.model.Teacher;
import com.team103.repository.AttendanceRepository;
import com.team103.repository.CourseRepository;
import com.team103.repository.StudentRepository;
import com.team103.repository.TeacherRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/manage/director")   // ✅ feature10 기준으로 고정
@CrossOrigin(origins = "*")
public class DirectorPeopleController {

    private final StudentRepository studentRepo;
    private final TeacherRepository teacherRepo;
    private final CourseRepository courseRepo;
    private final AttendanceRepository attendanceRepo;

    public DirectorPeopleController(
            StudentRepository studentRepo,
            TeacherRepository teacherRepo,
            CourseRepository courseRepo,
            AttendanceRepository attendanceRepo
    ) {
        this.studentRepo = studentRepo;
        this.teacherRepo = teacherRepo;
        this.courseRepo = courseRepo;
        this.attendanceRepo = attendanceRepo;
    }

    /* ======================== 공용 유틸 ======================== */

    private static String nz(Object v) { return v == null ? "" : String.valueOf(v); }

    /** Course → ClassLite 변환 (요일은 정수 리스트로 표준화) */
    private static ClassLite toClassLite(Course c) {
        // 항상 1~7 정수 배열로 변환된 값을 사용(코스 쪽에서 표준화 제공)
        List<Integer> days = c.getDaysOfWeekInt();
        return new ClassLite(
                nz(c.getId()),
                nz(getClassName(c)),
                new ArrayList<>(days),
                nz(c.getStartTime()),
                nz(c.getEndTime()),
                c.getRoomNumber() == null ? null : c.getRoomNumber()
        );
    }

    private static String getClassName(Course c) {
        try { return (String) Course.class.getMethod("getClassName").invoke(c); }
        catch (Exception ignore) {}
        try { return (String) Course.class.getMethod("getName").invoke(c); }
        catch (Exception ignore) {}
        return "";
    }

    /* ======================== DTO ======================== */

    public static final class StudentLite {
        public String studentId;
        public String name;
        public String school;
        public Integer grade;
        public StudentLite(String studentId, String name, String school, Integer grade) {
            this.studentId = studentId; this.name = name; this.school = school; this.grade = grade;
        }
    }

    public static final class TeacherLite {
        public String teacherId;
        public String name;
        public String phone;
        public Integer academyNumber;
        public TeacherLite(String teacherId, String name, String phone, Integer academyNumber) {
            this.teacherId = teacherId; this.name = name; this.phone = phone; this.academyNumber = academyNumber;
        }
    }

    public static final class ClassLite {
        public String classId;
        public String className;
        public List<Integer> dayOfWeek; // 1=월 … 7=일
        public String startTime;
        public String endTime;
        public Object roomNumber;
        public ClassLite(String classId, String className, List<Integer> dayOfWeek,
                         String startTime, String endTime, Object roomNumber) {
            this.classId = classId; this.className = className; this.dayOfWeek = dayOfWeek;
            this.startTime = startTime; this.endTime = endTime; this.roomNumber = roomNumber;
        }
    }

    public static final class AttendanceRow {
        public String date;
        public String classId;
        public String className;
        public String status;
        public AttendanceRow(String date, String classId, String className, String status) {
            this.date = date; this.classId = classId; this.className = className; this.status = status;
        }
    }

    /* ======================== 학생 목록/삭제 ======================== */

    @GetMapping("/students")
    @PreAuthorize("hasRole('DIRECTOR')")
    public List<StudentLite> listStudents() {
        return studentRepo.findAll().stream()
                .sorted(Comparator.comparing((Student s) -> nz(s.getStudentName()))
                        .thenComparing(s -> nz(s.getStudentId())))
                .map(s -> new StudentLite(
                        nz(s.getStudentId()),
                        nz(s.getStudentName()),
                        nz(s.getSchool()),
                        s.getGrade()
                ))
                .collect(Collectors.toList());
    }

    @DeleteMapping("/students/{studentId}")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ResponseEntity<?> deleteStudent(@PathVariable String studentId) {
        // 레포에 deleteByStudentId가 있으면 그대로 사용, 없으면 find→delete로 대체
        try {
            studentRepo.deleteByStudentId(studentId);
        } catch (Throwable t) {
            Student s = studentRepo.findByStudentId(studentId);
            if (s != null) studentRepo.delete(s);
        }
        return ResponseEntity.ok(Map.of("status","ok"));
    }

    /* ======================== 선생 목록/삭제 ======================== */

    @GetMapping("/teachers")
    @PreAuthorize("hasRole('DIRECTOR')")
    public List<TeacherLite> listTeachers() {
        return teacherRepo.findAll().stream()
                .sorted(Comparator.comparing((Teacher t) -> nz(t.getTeacherName()))
                        .thenComparing(t -> nz(t.getTeacherId())))
                .map(t -> new TeacherLite(
                        nz(t.getTeacherId()),
                        nz(t.getTeacherName()),
                        nz(t.getTeacherPhoneNumber()),
                        Optional.ofNullable(t.getAcademyNumbers()).filter(a -> !a.isEmpty()).map(a -> a.get(0)).orElse(null)
                ))
                .collect(Collectors.toList());
    }

    @DeleteMapping("/teachers/{teacherId}")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ResponseEntity<?> deleteTeacher(@PathVariable String teacherId) {
        try {
            teacherRepo.deleteByTeacherId(teacherId);
        } catch (Throwable t) {
            Teacher tch = teacherRepo.findByTeacherId(teacherId);
            if (tch != null) teacherRepo.delete(tch);
        }
        return ResponseEntity.ok(Map.of("status","ok"));
    }

    /* ======================== 학생: 수업/출결 (원장이 특정 학생 조회) ======================== */

    @GetMapping("/students/{studentId}/classes")
    @PreAuthorize("hasRole('DIRECTOR')")
    public List<ClassLite> listStudentClasses(@PathVariable String studentId) {
        List<Course> list = courseRepo.findByStudentsContaining(studentId);
        return list.stream()
                .sorted(Comparator
                        .comparing((Course c) -> nz(c.getStartTime()))
                        .thenComparing(DirectorPeopleController::getClassName))
                .map(DirectorPeopleController::toClassLite)
                .collect(Collectors.toList());
    }

    @GetMapping("/students/{studentId}/attendance")
    @PreAuthorize("hasRole('DIRECTOR')")
    public List<AttendanceRow> listStudentAttendance(@PathVariable String studentId) {
        Map<String,String> classNameMap = courseRepo.findByStudentsContaining(studentId)
                .stream()
                .collect(Collectors.toMap(c -> nz(c.getId()),
                        DirectorPeopleController::getClassName, (a,b)->a));

        List<Attendance> docs = attendanceRepo.findByStudentInAttendanceList(studentId);

        return docs.stream()
                .sorted(Comparator.comparing(Attendance::getDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .flatMap(att -> att.getAttendanceList().stream()
                        .filter(e -> studentId.equals(e.getStudentId()))
                        .map(e -> new AttendanceRow(
                                nz(att.getDate()),
                                nz(att.getClassId()),
                                classNameMap.getOrDefault(att.getClassId(), ""),
                                nz(e.getStatus())
                        )))
                .collect(Collectors.toList());
    }

    /* ======================== 선생: 수업/특정일 출결 (원장/교사 조회) ======================== */

    @GetMapping("/teachers/{teacherId}/classes")
    @PreAuthorize("hasAnyRole('DIRECTOR','TEACHER')")
    public List<ClassLite> listTeacherClasses(@PathVariable String teacherId) {
        List<Course> list = courseRepo.findByTeacherId(teacherId);
        return list.stream()
                .sorted(Comparator
                        .comparing((Course c) -> nz(c.getStartTime()))
                        .thenComparing(DirectorPeopleController::getClassName))
                .map(DirectorPeopleController::toClassLite)
                .collect(Collectors.toList());
    }

    @GetMapping("/teachers/classes/{classId}/attendance")
    @PreAuthorize("hasAnyRole('DIRECTOR','TEACHER')")
    public List<AttendanceRow> classAttendanceByDate(
            @PathVariable String classId,
            @RequestParam String date
    ) {
        return attendanceRepo.findByClassIdAndDate(classId, date).stream()
                .flatMap(att -> att.getAttendanceList().stream()
                        .map(e -> new AttendanceRow(
                                nz(att.getDate()),
                                nz(att.getClassId()),
                                nz(getClassName(courseRepo.getByClassIdOrNull(classId))),
                                nz(e.getStatus())
                        )))
                .collect(Collectors.toList());
    }
}
