package com.team103.controller;

import com.team103.dto.ClassIdNameDto;
import com.team103.model.Course;
import com.team103.model.Parent;
import com.team103.model.Student;
import com.team103.model.Teacher;
import com.team103.repository.CourseRepository;
import com.team103.repository.ParentRepository;
import com.team103.repository.StudentRepository;
import com.team103.repository.TeacherRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/lookup/classes")
@CrossOrigin(origins = "*")
public class ClassLookupController {

    private final CourseRepository courseRepo;
    private final ParentRepository parentRepo;
    private final StudentRepository studentRepo;
    private final TeacherRepository teacherRepo;

    public ClassLookupController(CourseRepository courseRepo,
                                 ParentRepository parentRepo,
                                 StudentRepository studentRepo,
                                 TeacherRepository teacherRepo) {
        this.courseRepo = courseRepo;
        this.parentRepo = parentRepo;
        this.studentRepo = studentRepo;
        this.teacherRepo = teacherRepo;
    }

    /** 학생 기준 라이트 목록 (name = 과목명(교사이름)) */
    @GetMapping("/by-student/{studentId}")
    public ResponseEntity<List<ClassIdNameDto>> byStudent(@PathVariable String studentId) {
        List<Course> courses = courseRepo.findByStudentsContaining(studentId);
        return ResponseEntity.ok(toLiteDistinctWithTeacher(courses));
    }

    /** 학부모 기준(자녀 합집합) 라이트 목록 (name = 과목명(교사이름)) */
    @GetMapping("/by-parent/{parentId}")
    public ResponseEntity<List<ClassIdNameDto>> byParent(@PathVariable String parentId) {
        Parent p = parentRepo.findByParentsId(parentId);
        if (p == null) return ResponseEntity.ok(List.of());

        // 자녀 수집: studentIds 우선 → 없으면 Parents_Number 폴백
        List<Student> kids = new ArrayList<>();
        List<String> childIds = p.getStudentIds();
        if (childIds != null && !childIds.isEmpty()) {
            for (String sid : childIds) {
                if (sid == null || sid.isBlank()) continue;
                Student s = studentRepo.findByStudentId(sid);
                if (s != null) kids.add(s);
            }
        } else {
            String pn = p.getParentsNumber();
            if (pn != null && !pn.isBlank()) {
                kids.addAll(studentRepo.findByParentsNumber(pn));
            }
        }

        // 자녀들의 과목 합집합 → 라이트 중복 제거 + 교사이름 포함
        LinkedHashMap<String, ClassIdNameDto> map = new LinkedHashMap<>();
        for (Student s : kids) {
            if (s == null || s.getStudentId() == null) continue;
            List<Course> cs = courseRepo.findByStudentsContaining(s.getStudentId());
            for (ClassIdNameDto dto : toLiteDistinctWithTeacher(cs)) {
                String id = dto.getId();
                if (id == null || id.isBlank()) continue;
                ClassIdNameDto prev = map.get(id);
                // 기존이 비어있거나, 기존 name이 비었을 때만 대체
                if (prev == null || isBlank(prev.getName()) && !isBlank(dto.getName())) {
                    map.put(id, dto);
                }
            }
        }
        return ResponseEntity.ok(new ArrayList<>(map.values()));
    }

    /** id → 이름 보강(일괄). name = 과목명(교사이름) */
    @GetMapping("/names")
    public ResponseEntity<List<ClassIdNameDto>> names(@RequestParam("ids") String ids) {
        if (ids == null || ids.isBlank()) return ResponseEntity.ok(List.of());
        String[] arr = ids.split(",");
        LinkedHashMap<String, ClassIdNameDto> map = new LinkedHashMap<>();

        // 간단 캐시: teacherId → teacherName
        Map<String, String> teacherNameCache = new HashMap<>();

        for (String raw : arr) {
            String id = raw == null ? null : raw.trim();
            if (id == null || id.isEmpty() || map.containsKey(id)) continue;

            // 1) Class_ID로 조회 → 2) Mongo _id로 조회
            Course c = courseRepo.findByClassId(id).orElseGet(() ->
                    courseRepo.findById(id).orElse(null)
            );
            String label = null;
            if (c != null) {
                String base = nz(c.getClassName(), id); // 과목명 없으면 id
                String tName = resolveTeacherName(c.getTeacherId(), teacherNameCache);
                label = appendTeacher(base, tName);
            }
            map.put(id, new ClassIdNameDto(id, label));
        }
        return ResponseEntity.ok(new ArrayList<>(map.values()));
    }

    // ===== 내부 유틸 =====

    /** 과목 라이트 + 교사이름 포함, id 기준 중복 제거 */
    private List<ClassIdNameDto> toLiteDistinctWithTeacher(List<Course> courses) {
        LinkedHashMap<String, ClassIdNameDto> map = new LinkedHashMap<>();
        Map<String, String> teacherNameCache = new HashMap<>();

        for (Course c : courses) {
            if (c == null) continue;
            String id = nz(c.getClassId(), c.getId());
            if (isBlank(id)) continue;

            String base = nz(c.getClassName(), id); // 과목명 없으면 id
            String tName = resolveTeacherName(c.getTeacherId(), teacherNameCache);
            String label = appendTeacher(base, tName);

            ClassIdNameDto dto = new ClassIdNameDto(id, label);

            ClassIdNameDto prev = map.get(id);
            if (prev == null || (isBlank(prev.getName()) && !isBlank(dto.getName()))) {
                map.put(id, dto);
            }
        }
        return new ArrayList<>(map.values());
    }

    /** teacherId → teacherName 간단 캐시 조회 */
    private String resolveTeacherName(String teacherId, Map<String, String> cache) {
        if (isBlank(teacherId)) return null;
        if (cache.containsKey(teacherId)) return cache.get(teacherId);
        Teacher t = teacherRepo.findByTeacherId(teacherId);
        String name = (t != null) ? t.getTeacherName() : null;
        cache.put(teacherId, name);
        return name;
    }

    private static String appendTeacher(String base, String teacherName) {
        if (isBlank(base)) base = null;
        if (isBlank(teacherName)) return base;
        if (base == null) return "(" + teacherName + ")";
        return base + "(" + teacherName + ")";
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String nz(String a, String b) {
        if (!isBlank(a)) return a;
        if (!isBlank(b)) return b;
        return null;
    }
}
