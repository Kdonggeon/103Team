// src/main/java/com/team103/controller/ParentController.java
package com.team103.controller;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Criteria;

import com.team103.dto.AddChildrenRequest;
import com.team103.dto.FindIdRequest;
import com.team103.dto.ParentSignupRequest;
import com.team103.dto.ChildSummary;

import com.team103.model.Attendance;
import com.team103.model.Course;
import com.team103.model.Parent;
import com.team103.model.Student;

import com.team103.repository.AttendanceRepository;
import com.team103.repository.CourseRepository;
import com.team103.repository.ParentRepository;
import com.team103.repository.StudentRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping("/api/parents")
@CrossOrigin(origins = "*")
public class ParentController {

    private final ParentRepository parentRepo;

    @Autowired
    private MongoTemplate mongo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StudentRepository studentRepo;

    @Autowired
    private CourseRepository courseRepo;

    @Autowired
    private AttendanceRepository attendanceRepo;

    public ParentController(ParentRepository parentRepo) {
        this.parentRepo = parentRepo;
    }

    /** ───────────────────────── 공용 DTO ───────────────────────── */
    public static class UpdateAcademiesRequest {
        private List<Integer> add;
        private List<Integer> remove;
        public List<Integer> getAdd() { return add; }
        public void setAdd(List<Integer> add) { this.add = add; }
        public List<Integer> getRemove() { return remove; }
        public void setRemove(List<Integer> remove) { this.remove = remove; }
    }
    public static class ReplaceAcademiesRequest {
        private List<Integer> academies;
        public List<Integer> getAcademies() { return academies; }
        public void setAcademies(List<Integer> academies) { this.academies = academies; }
    }

    /** 학원 존재 여부(컬렉션명이 프로젝트마다 달 수 있어 둘 다 확인) */
    private boolean academyExists(Integer academyNumber) {
        if (academyNumber == null) return false;
        Query q = new Query(Criteria.where("academyNumber").is(academyNumber));
        return mongo.exists(q, "academy") || mongo.exists(q, "academies");
    }

    /** ───────────────────────── 기본/인증 관련 ───────────────────────── */
    /** 학부모 전체 조회 */
    @GetMapping
    public List<Parent> getAllParents() {
        return parentRepo.findAll();
    }

    /** FCM 토큰 업데이트 */
    @PutMapping("/{id}/fcm-token")
    public ResponseEntity<Void> updateFcmToken(@PathVariable("id") String parentsId,
                                               @RequestParam("token") String token) {
        Parent p = parentRepo.findByParentsId(parentsId);
        if (p == null) return ResponseEntity.notFound().build();
        p.setFcmToken((token == null || token.isBlank()) ? null : token);
        parentRepo.save(p);
        return ResponseEntity.ok().build();
    }

    /** 학부모 회원가입 */
    @PostMapping
    public ResponseEntity<?> registerParent(@RequestBody ParentSignupRequest request) {
        if (request.getParentsPw() == null || request.getParentsPw().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("비밀번호는 필수 입력 항목입니다.");
        }
        String encodedPw = passwordEncoder.encode(request.getParentsPw());
        Parent parent = request.toEntity(encodedPw);
        Parent saved = parentRepo.save(parent);
        return ResponseEntity.ok(saved);
    }

    /** 학부모 ID로 단건 조회 */
    @GetMapping("/{id}")
    public Parent getById(@PathVariable String id) {
        return parentRepo.findByParentsId(id);
    }

    /** ───────────────────────── 자녀 등록/해제 + 합집합 동기화 ───────────────────────── */
    /** 자녀(단일/여러 명) 추가: 존재 검증(404) + 중복(409) 처리 */
    @PostMapping("/{parentId}/children")
    public ResponseEntity<?> addChildren(
            @PathVariable String parentId,
            @RequestBody(required = false) AddChildrenRequest request,
            @RequestParam(value = "studentId", required = false) String qsStudentId
    ) {
        Parent parent = parentRepo.findByParentsId(parentId);
        if (parent == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("부모 정보를 찾을 수 없습니다.");
        }

        // 입력값 수집/정규화/중복제거(순서보존)
        LinkedHashSet<String> in = new LinkedHashSet<>();
        if (request != null) {
            for (String s : request.normalizedIds()) {
                if (s != null && !s.isBlank()) in.add(s.trim());
            }
        }
        if (qsStudentId != null && !qsStudentId.isBlank()) in.add(qsStudentId.trim());

        if (in.isEmpty()) {
            return ResponseEntity.badRequest().body("studentId 또는 studentIds가 필요합니다.");
        }

        List<String> incoming = new ArrayList<>(in);

        // 존재 검증
        List<Student> found = studentRepo.findByStudentIdIn(incoming);
        Set<String> foundIds = (found == null ? new HashSet<>() :
                found.stream().map(Student::getStudentId).collect(Collectors.toSet()));
        List<String> missing = incoming.stream()
                .filter(id -> !foundIds.contains(id))
                .collect(Collectors.toList());
        if (!missing.isEmpty()) {
            // 프런트는 status 404만 보고 "존재하지 않는 학생입니다."로 매핑함
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "존재하지 않는 학생입니다.", "invalidStudentIds", missing));
        }

        // 중복 검증 (람다 캡처 문제 방지용 별도 final 참조)
        List<String> current = parent.getStudentIds();
        if (current == null) current = new ArrayList<>();
        final Set<String> currentSet = new HashSet<>(current);

        List<String> toAdd = incoming.stream()
                .filter(id -> !currentSet.contains(id))
                .collect(Collectors.toList());
        if (toAdd.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 등록된 자녀입니다.");
        }

        // 저장
        current.addAll(toAdd);
        parent.setStudentIds(current);
        parentRepo.save(parent);

        // 응답용 요약
        List<ChildSummary> createdSummaries = found.stream()
                .filter(s -> toAdd.contains(s.getStudentId()))
                .map(this::toSummary)
                .collect(Collectors.toList());

        // 자녀 기반 학원번호 합집합 계산 → 부모 문서에 저장
        Set<Integer> union = computeAcademyUnionForParent(parent);
        parent.setAcademyNumbers(new ArrayList<>(union));
        parentRepo.save(parent);

        if (createdSummaries.size() == 1) {
            return ResponseEntity.ok(Map.of(
                    "created", createdSummaries.get(0),
                    "academyNumbersUnion", new ArrayList<>(union)
            ));
        }
        return ResponseEntity.ok(Map.of(
                "created", createdSummaries,
                "academyNumbersUnion", new ArrayList<>(union)
        ));
    }

    /** 자녀 연결 해제 */
    @DeleteMapping("/{parentId}/children/{studentId}")
    public ResponseEntity<?> removeChild(@PathVariable String parentId,
                                         @PathVariable String studentId) {
        Parent parent = parentRepo.findByParentsId(parentId);
        if (parent == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("부모 정보를 찾을 수 없습니다.");
        }

        // 1) 부모의 studentIds 목록에서 제거
        List<String> ids = parent.getStudentIds();
        if (ids != null) {
            ids.removeIf(s -> s != null && s.equals(studentId));
            parent.setStudentIds(ids);
            parentRepo.save(parent);
        }

        // 2) 학생 문서의 연결 해제(선택 사항)
        Student s = studentRepo.findByStudentId(studentId);
        if (s != null && s.getParentsNumber() != null
                && s.getParentsNumber().equals(parent.getParentsNumber())) {
            s.setParentsNumber(null);
            studentRepo.save(s);
        }

        // 합집합 재계산 동기화
        Set<Integer> union = computeAcademyUnionForParent(parent);
        parent.setAcademyNumbers(new ArrayList<>(union));
        parentRepo.save(parent);

        return ResponseEntity.noContent().build();
    }

    /** 자녀 1명 추가: 존재 검증(404) + 중복(409) */
    @PostMapping("/{parentId}/children/{studentId}")
    public ResponseEntity<?> addOneChild(@PathVariable String parentId,
                                         @PathVariable String studentId) {
        Parent parent = parentRepo.findByParentsId(parentId);
        if (parent == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("부모 정보를 찾을 수 없습니다.");
        }

        Student s = studentRepo.findByStudentId(studentId);
        if (s == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("존재하지 않는 학생입니다.");
        }

        List<String> ids = parent.getStudentIds();
        if (ids == null) ids = new ArrayList<>();
        if (ids.contains(studentId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 등록된 자녀입니다.");
        }

        ids.add(studentId);
        parent.setStudentIds(ids);
        parentRepo.save(parent);

        // 자녀 기반 학원번호 합집합 계산 → 부모 문서에 저장
        Set<Integer> union = computeAcademyUnionForParent(parent);
        parent.setAcademyNumbers(new ArrayList<>(union));
        parentRepo.save(parent);

        return ResponseEntity.ok(Map.of(
                "ok", true,
                "academyNumbersUnion", new ArrayList<>(union)
        ));
    }

    /** ───────────────────────── 학부모→자녀 학원번호 관리 ───────────────────────── */
    /** 권한 확인: 부모가 해당 자녀를 관리할 수 있는지 */
    private boolean canManageChild(Parent parent, Student s) {
        if (parent == null || s == null) return false;
        List<String> ids = parent.getStudentIds();
        if (ids != null && ids.contains(s.getStudentId())) return true;
        String pn = parent.getParentsNumber();
        return pn != null && pn.equals(s.getParentsNumber());
    }

    /** 부분 수정: add/remove (add는 DB 존재 검증 필수) */
    @PatchMapping("/{parentId}/children/{studentId}/academies")
    public ResponseEntity<?> patchChildAcademies(@PathVariable String parentId,
                                                 @PathVariable String studentId,
                                                 @RequestBody(required = false) UpdateAcademiesRequest req) {
        Parent parent = parentRepo.findByParentsId(parentId);
        if (parent == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("부모 정보를 찾을 수 없습니다.");

        Student s = studentRepo.findByStudentId(studentId);
        if (s == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("학생 정보를 찾을 수 없습니다.");

        if (!canManageChild(parent, s)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("권한이 없습니다.");

        Set<Integer> set = new HashSet<>();
        List<Integer> existing = s.getAcademyNumbers();
        if (existing != null) for (Integer a : existing) if (a != null) set.add(a);

        // 유효성 검사: add에 담긴 번호가 모두 DB에 존재하는지
        if (req != null && req.getAdd() != null && !req.getAdd().isEmpty()) {
            List<Integer> invalid = req.getAdd().stream()
                    .filter(a -> a != null && !academyExists(a))
                    .collect(Collectors.toList());
            if (!invalid.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "존재하지 않는 학원번호가 포함되어 있습니다.",
                                     "invalidAcademyNumbers", invalid));
            }
        }

        if (req != null) {
            if (req.getAdd() != null) {
                for (Integer a : req.getAdd()) {
                    if (a != null && academyExists(a)) set.add(a);
                }
            }
            if (req.getRemove() != null) {
                for (Integer a : req.getRemove()) {
                    if (a != null) set.remove(a);
                }
            }
        }

        s.setAcademyNumbers(new ArrayList<>(set));
        studentRepo.save(s);

        // 부모 합집합도 갱신
        Set<Integer> union = computeAcademyUnionForParent(parent);
        parent.setAcademyNumbers(new ArrayList<>(union));
        parentRepo.save(parent);

        return ResponseEntity.ok(toSummary(s));
    }

    /** 전체 교체: academies 배열로 바꿔치기 (모두 DB 존재해야 함) */
    @PutMapping("/{parentId}/children/{studentId}/academies")
    public ResponseEntity<?> replaceChildAcademies(@PathVariable String parentId,
                                                   @PathVariable String studentId,
                                                   @RequestBody ReplaceAcademiesRequest req) {
        Parent parent = parentRepo.findByParentsId(parentId);
        if (parent == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("부모 정보를 찾을 수 없습니다.");

        Student s = studentRepo.findByStudentId(studentId);
        if (s == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("학생 정보를 찾을 수 없습니다.");

        if (!canManageChild(parent, s)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("권한이 없습니다.");

        Set<Integer> set = new HashSet<>();
        if (req != null && req.getAcademies() != null) {
            List<Integer> invalid = req.getAcademies().stream()
                    .filter(a -> a != null && !academyExists(a))
                    .collect(Collectors.toList());
            if (!invalid.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "존재하지 않는 학원번호가 포함되어 있습니다.",
                                     "invalidAcademyNumbers", invalid));
            }
            for (Integer a : req.getAcademies()) if (a != null) set.add(a);
        }

        s.setAcademyNumbers(new ArrayList<>(set));
        studentRepo.save(s);

        // 부모 합집합도 갱신
        Set<Integer> union = computeAcademyUnionForParent(parent);
        parent.setAcademyNumbers(new ArrayList<>(union));
        parentRepo.save(parent);

        return ResponseEntity.ok(toSummary(s));
    }

    /** 학부모가 접근 가능한 학원번호 합집합(자녀 기반 계산) */
    @GetMapping("/{parentId}/academies")
    public ResponseEntity<?> getAcademiesUnion(@PathVariable String parentId) {
        Parent parent = parentRepo.findByParentsId(parentId);
        if (parent == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("부모 정보를 찾을 수 없습니다.");
        }
        Set<Integer> union = computeAcademyUnionForParent(parent);

        // 최신 계산값으로 부모 문서 동기화(안전)
        parent.setAcademyNumbers(new ArrayList<>(union));
        parentRepo.save(parent);

        return ResponseEntity.ok(Map.of(
                "academyNumbersUnion", new ArrayList<>(union)
        ));
    }

    /** ───────────────────────── 조회 보조 ───────────────────────── */
    /** 특정 자녀의 수업 목록 조회 */
    @GetMapping("/{parentId}/children/{studentId}/classes")
    public ResponseEntity<?> getChildClasses(@PathVariable String parentId,
                                             @PathVariable String studentId) {
        List<Course> classes = courseRepo.findByStudentsContaining(studentId);
        return ResponseEntity.ok(classes);
    }

    /** 학부모의 모든 자녀 출석 내역 조회 */
    @GetMapping("/{parentId}/attendance")
    public ResponseEntity<?> getChildAttendance(@PathVariable String parentId) {
        Parent parent = parentRepo.findByParentsId(parentId);
        if (parent == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("부모님 정보 없음");
        }

        String parentNumber = parent.getParentsNumber();
        List<Student> children = studentRepo.findByParentsNumber(parentNumber);

        List<Attendance> allAttendance = new ArrayList<>();
        for (Student child : children) {
            List<Attendance> attendance = attendanceRepo.findByStudentInAttendanceList(child.getStudentId());
            allAttendance.addAll(attendance);
        }

        return ResponseEntity.ok(allAttendance);
    }
    
    

    /** 학부모의 자녀 목록 조회 */
    @GetMapping("/{parentId}/children")
    public ResponseEntity<?> getChildren(@PathVariable String parentId) {
        List<Student> kids = findChildrenInternal(parentId);
        List<ChildSummary> result = kids.stream().map(this::toSummary).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /** 별칭 엔드포인트 (동일 로직) */
    @GetMapping("/{parentId}/students")
    public ResponseEntity<?> getChildrenAlias(@PathVariable String parentId) {
        List<Student> kids = findChildrenInternal(parentId);
        List<ChildSummary> result = kids.stream().map(this::toSummary).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /** 공통 로직: studentIds 우선 → 없으면 parentsNumber 대체 */
    private List<Student> findChildrenInternal(String parentId) {
        Parent parent = parentRepo.findByParentsId(parentId);
        if (parent == null) return new ArrayList<>();

        // 1) studentIds 우선
        List<String> studentIds = parent.getStudentIds();
        if (studentIds != null && !studentIds.isEmpty()) {
            List<Student> s = studentRepo.findByStudentIdIn(studentIds);
            if (s != null && !s.isEmpty()) return s;
        }

        // 2) 없거나 비면 parentsNumber 대체
        String parentNumber = parent.getParentsNumber();
        if (parentNumber != null && !parentNumber.isBlank()) {
            List<Student> s = studentRepo.findByParentsNumber(parentNumber);
            if (s != null) return s;
        }
        return new ArrayList<>();
    }

    /** 학부모의 자녀 이름 목록 조회 */
    @GetMapping("/{parentId}/children/names")
    public ResponseEntity<?> getChildNames(@PathVariable String parentId) {
        Parent parent = parentRepo.findByParentsId(parentId);
        if (parent == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("부모 정보를 찾을 수 없습니다.");
        }

        List<Student> students = new ArrayList<>();

        List<String> studentIds = parent.getStudentIds();
        if (studentIds != null && !studentIds.isEmpty()) {
            students = studentRepo.findByStudentIdIn(studentIds);
        }
        if (students == null || students.isEmpty()) {
            String parentNumber = parent.getParentsNumber();
            if (parentNumber != null && !parentNumber.isBlank()) {
                students = studentRepo.findByParentsNumber(parentNumber);
            }
        }

        List<String> studentNames = new ArrayList<>();
        if (students != null) {
            for (Student s : students) {
                studentNames.add(s.getStudentName());
            }
        }
        return ResponseEntity.ok(studentNames);
    }

    /** 아이디 찾기 (이름 + 전화번호) */
    @PostMapping("/find_id")
    public ResponseEntity<Map<String,String>> findParentId(@RequestBody FindIdRequest req) {
        String phone = req.normalizedPhone();
        Parent p = parentRepo.findByParentsNameAndParentsPhoneNumber(req.getName(), phone);
        if (p == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "not found");
        return ResponseEntity.ok(Map.of("username", p.getParentsId()));
    }

    /** ───────────────────────── 내부 유틸(요약/합집합) ───────────────────────── */
    /** ChildSummary 생성 (학생·수업 기반으로 학원번호 보강) */
    private ChildSummary toSummary(Student s) {
        Set<Integer> academies = collectAcademiesForStudent(s);
        ChildSummary dto = new ChildSummary();
        dto.setStudentId(s.getStudentId());
        dto.setStudentName(s.getStudentName());
        dto.setAcademies(new ArrayList<>(academies));
        return dto;
    }

    /** 학생 1명의 학원번호 수집 (Student + Course) */
    private Set<Integer> collectAcademiesForStudent(Student s) {
        Set<Integer> academies = new HashSet<>();

        // 1) Student 문서의 academyNumbers
        try {
            List<Integer> arr = s.getAcademyNumbers();
            if (arr != null) for (Integer v : arr) if (v != null) academies.add(v);
        } catch (Exception ignore) {}

        // 2) 수업(Course) 기반 보강
        List<Course> courses = courseRepo.findByStudentsContaining(s.getStudentId());
        if (courses != null) {
            for (Course c : courses) {
                try {
                    // 다중 필드가 있으면 우선 사용
                    List<Integer> many = c.getAcademyNumbersSafe();
                    if (many != null) {
                        for (Integer v : many) if (v != null) academies.add(v);
                        continue;
                    }
                } catch (Exception ignore) {}

                try {
                    // 단일 필드도 보강
                    Integer one = c.getAcademyNumber();
                    if (one != null) academies.add(one);
                } catch (Exception ignore) {}
            }
        }
        return academies;
    }

    /** 부모의 모든 자녀를 기준으로 학원번호 합집합 계산 */
    private Set<Integer> computeAcademyUnionForParent(Parent parent) {
        Set<Integer> union = new HashSet<>();
        List<Student> kids = findChildrenInternal(parent.getParentsId());
        if (kids != null) {
            for (Student s : kids) {
                union.addAll(collectAcademiesForStudent(s));
            }
        }
        return union;
    }

    /** 자녀에 학원번호 1개 추가 (단건) — 반드시 DB 존재해야 함 */
    @PostMapping("/{parentId}/children/{studentId}/academies/{academyNumber}")
    public ResponseEntity<?> addAcademyToChild(@PathVariable String parentId,
                                               @PathVariable String studentId,
                                               @PathVariable Integer academyNumber) {
        Parent parent = parentRepo.findByParentsId(parentId);
        if (parent == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("부모 정보를 찾을 수 없습니다.");

        Student s = studentRepo.findByStudentId(studentId);
        if (s == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("학생 정보를 찾을 수 없습니다.");

        // 서버측 유효성: 학원번호가 실제 DB에 존재하는지 확인
        if (!academyExists(academyNumber)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("유효하지 않은 학원번호입니다. DB에 존재하는 학원만 등록할 수 있습니다.");
        }

        // 접근 제어(해당 부모의 자녀인지 확인)
        List<String> ids = parent.getStudentIds();
        boolean linked = (ids != null && ids.contains(studentId))
                || (parent.getParentsNumber() != null
                    && parent.getParentsNumber().equals(s.getParentsNumber()));
        if (!linked) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("해당 자녀에 대한 권한이 없습니다.");

        // 실제 추가
        List<Integer> arr = s.getAcademyNumbers();
        if (arr == null) arr = new ArrayList<>();
        if (academyNumber != null && !arr.contains(academyNumber)) arr.add(academyNumber);
        s.setAcademyNumbers(arr);
        studentRepo.save(s);

        // 부모 합집합 갱신
        Set<Integer> union = computeAcademyUnionForParent(parent);
        parent.setAcademyNumbers(new ArrayList<>(union));
        parentRepo.save(parent);

        return ResponseEntity.ok(Map.of(
                "child", toSummary(s),
                "academyNumbersUnion", new ArrayList<>(union)
        ));
    }
}
