package com.team103.controller;

import com.team103.dto.FindIdRequest;
import com.team103.dto.DirectorSignupRequest;
import com.team103.model.Director;
import com.team103.repository.DirectorRepository;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/directors")
@CrossOrigin(origins = "*")
public class DirectorController {

    private final DirectorRepository directorRepo;
    private final PasswordEncoder passwordEncoder;
    private final MongoTemplate mongo;

    @Autowired
    public DirectorController(
            DirectorRepository directorRepo,
            PasswordEncoder passwordEncoder,
            MongoTemplate mongo
    ) {
        this.directorRepo = directorRepo;
        this.passwordEncoder = passwordEncoder;
        this.mongo = mongo;
    }

    /** 원장 회원가입 */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody DirectorSignupRequest req) {
        if (req.getUsername() == null || req.getUsername().isBlank())
            return ResponseEntity.badRequest().body("username is required");
        if (req.getPassword() == null || req.getPassword().isBlank())
            return ResponseEntity.badRequest().body("password is required");

        if (directorRepo.existsByUsername(req.getUsername())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("username already exists");
        }

        String encodedPw = passwordEncoder.encode(req.getPassword());

        Director d = new Director();
        d.setName(req.getName());
        d.setUsername(req.getUsername());
        d.setPassword(encodedPw);
        d.setPhone(req.getPhone());
        d.setAcademyNumbers(req.getAcademyNumbers());
        directorRepo.save(d);

        return ResponseEntity.status(HttpStatus.CREATED).body("director signup success");
    }

    /** 원장 단건 조회 (username 기준) */
    @GetMapping("/{username}")
    public ResponseEntity<Director> getByUsername(@PathVariable String username) {
        Director d = directorRepo.findByUsername(username);
        if (d == null) return ResponseEntity.notFound().build();
        d.setPassword(null);
        return ResponseEntity.ok(d);
    }

    /** 아이디 찾기 (이름 + 전화번호 → username 반환) */
    @PostMapping("/find_id")
    public ResponseEntity<Map<String,String>> findDirectorId(@RequestBody FindIdRequest req) {
        String phone = req.normalizedPhone();
        Director d = directorRepo.findByNameAndPhone(req.getName(), phone);
        if (d == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "not found");
        return ResponseEntity.ok(Map.of("username", d.getUsername()));
    }

    /** 로그인한 원장 본인 정보 */
    @GetMapping("/me")
    public ResponseEntity<DirectorMe> me(
            Authentication authentication,
            @RequestParam(value = "username", required = false) String usernameFallback
    ) {
        String username = resolveUsername(authentication, usernameFallback);
        Director d = directorRepo.findByUsername(username);
        if (d == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "director not found");

        DirectorMe me = new DirectorMe();
        me.setUsername(d.getUsername());
        me.setName(d.getName());
        me.setPhone(d.getPhone());
        me.setAcademyNumbers(
                d.getAcademyNumbers() != null ? d.getAcademyNumbers() : Collections.emptyList()
        );
        return ResponseEntity.ok(me);
    }

    /** 소속 학원 요약 조회: GET /api/directors/academies?numbers=103,105 */
    @GetMapping("/academies")
    public ResponseEntity<List<AcademySummary>> academies(@RequestParam("numbers") String numbersParam) {
        if (numbersParam == null || numbersParam.isBlank()) return ResponseEntity.ok(List.of());

        Set<Integer> numbers = Arrays.stream(numbersParam.split("[,\\s]+"))
                .filter(s -> !s.isBlank())
                .map(s -> {
                    try { return Integer.parseInt(s.trim()); }
                    catch (NumberFormatException e) { return null; }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (numbers.isEmpty()) return ResponseEntity.ok(List.of());

        List<AcademySummary> result = new ArrayList<>();
        for (Integer n : numbers) {
            AcademySummary a = findAcademyByNumber(n);
            if (a != null) result.add(a);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 소속 학원 정보 수정(부분 업데이트):
     * PATCH /api/directors/academies/{academyNumber}
     * body: { "name"?: "...", "address"?: "...", "phone"?: "..." }
     * - 로그인한 원장이 해당 academyNumber를 보유해야 수정 가능(권한 체크)
     * - 컬렉션/필드명이 제각각일 수 있어, 기존에 탐지된 컬렉션/필드명에 맞춰 set
     */
    @PatchMapping("/academies/{academyNumber}")
    public ResponseEntity<AcademySummary> updateAcademy(
            @PathVariable Integer academyNumber,
            @RequestBody Map<String, Object> body,
            Authentication authentication,
            @RequestParam(value = "username", required = false) String usernameFallback
    ) {
        String username = resolveUsername(authentication, usernameFallback);
        Director dir = directorRepo.findByUsername(username);
        if (dir == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "director not found");

        List<Integer> mine = dir.getAcademyNumbers() != null ? dir.getAcademyNumbers() : List.of();
        if (!mine.contains(academyNumber)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "no permission for this academy");
        }

        // 변경할 값 파싱
        String newName = str(body.get("name"));
        String newAddress = str(body.get("address"));
        String newPhone = normalizePhone(str(body.get("phone")));

        // 우선 기존 문서를 어떤 컬렉션/필드에 갖고 있는지 찾고, 같은 위치에 업데이트
        TargetDoc target = findTargetDoc(academyNumber);
        if (target == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "academy not found");

        Update u = new Update();
        boolean hasUpdate = false;

        // name
        if (newName != null) {
            for (String k : List.of("name", "academyName", "Academy_Name", "title")) {
                if (target.doc.containsKey(k)) {
                    u.set(k, newName);
                    hasUpdate = true;
                    break;
                }
            }
            if (!hasUpdate) { // 기존 키가 없으면 대표 키로 set
                u.set("name", newName);
                hasUpdate = true;
            }
        }

        // address
        if (newAddress != null) {
            boolean set = false;
            for (String k : List.of("address", "Academy_Address", "addr")) {
                if (target.doc.containsKey(k)) {
                    u.set(k, newAddress);
                    set = true;
                    break;
                }
            }
            if (!set) u.set("address", newAddress);
            hasUpdate = true;
        }

        // phone
        if (newPhone != null) {
            boolean set = false;
            for (String k : List.of("phone", "Academy_Phone_Number", "tel", "phoneNumber")) {
                if (target.doc.containsKey(k)) {
                    u.set(k, newPhone);
                    set = true;
                    break;
                }
            }
            if (!set) u.set("phone", newPhone);
            hasUpdate = true;
        }

        if (!hasUpdate) {
            // 변경할 필드가 없으면 그대로 반환
            return ResponseEntity.ok(fromDoc(target.doc));
        }

        Query q = Query.query(Criteria.where(target.numberField).is(academyNumber));
        mongo.updateFirst(q, u, target.collection);

        // 다시 읽어서 최신값 반환
        Document updated = mongo.findOne(q, Document.class, target.collection);
        AcademySummary summary = fromDoc(updated);
        if (summary == null) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "update failed");
        return ResponseEntity.ok(summary);
    }

    /* ============================ 내부 유틸 ============================ */

    private String resolveUsername(Authentication auth, String fallback) {
        if (auth != null && auth.getName() != null && !auth.getName().isBlank()) return auth.getName();
        if (fallback != null && !fallback.isBlank()) return fallback;
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized");
    }

    private String str(Object o) {
        if (o == null) return null;
        String s = String.valueOf(o).trim();
        return s.isEmpty() ? null : s;
    }

    private String normalizePhone(String p) {
        if (p == null) return null;
        String digits = p.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return null;
        return digits;
    }

    private static class TargetDoc {
        String collection;
        String numberField; // academyNumber / Academy_Number / academy_number
        Document doc;
    }

    private TargetDoc findTarget(String collection, String numberField, Integer num) {
        try {
            Query q = Query.query(Criteria.where(numberField).is(num));
            Document d = mongo.findOne(q, Document.class, collection);
            if (d == null) return null;
            TargetDoc t = new TargetDoc();
            t.collection = collection;
            t.numberField = numberField;
            t.doc = d;
            return t;
        } catch (Exception e) { return null; }
    }

    private TargetDoc findTargetDoc(Integer num) {
        for (String c : List.of("academies", "academy", "rooms")) {
            for (String f : List.of("academyNumber", "Academy_Number", "academy_number")) {
                TargetDoc t = findTarget(c, f, num);
                if (t != null) return t;
            }
        }
        return null;
    }

    private AcademySummary findAcademyByNumber(Integer num) {
        Document doc = null;
        for (String c : List.of("academies", "academy", "rooms")) {
            for (String f : List.of("academyNumber", "Academy_Number", "academy_number")) {
                try {
                    Query q = Query.query(Criteria.where(f).is(num));
                    doc = mongo.findOne(q, Document.class, c);
                    if (doc != null) return fromDoc(doc);
                } catch (Exception ignore) {}
            }
        }
        return null;
    }

    private AcademySummary fromDoc(Document doc) {
        if (doc == null) return null;
        AcademySummary a = new AcademySummary();

        Integer num = null;
        Object n1 = doc.get("academyNumber");
        Object n2 = doc.get("Academy_Number");
        Object n3 = doc.get("academy_number");
        if (n1 instanceof Number) num = ((Number) n1).intValue();
        else if (n2 instanceof Number) num = ((Number) n2).intValue();
        else if (n3 instanceof Number) num = ((Number) n3).intValue();
        a.setAcademyNumber(num);

        String name = firstNonEmpty(
                doc.getString("name"),
                doc.getString("academyName"),
                doc.getString("Academy_Name"),
                doc.getString("title")
        );
        a.setName(name);

        String address = firstNonEmpty(
                doc.getString("address"),
                doc.getString("Academy_Address"),
                doc.getString("addr")
        );
        a.setAddress(address);

        String phone = firstNonEmpty(
                doc.getString("phone"),
                doc.getString("Academy_Phone_Number"),
                doc.getString("tel"),
                doc.getString("phoneNumber")
        );
        a.setPhone(phone);

        return a.getAcademyNumber() != null ? a : null;
    }

    private String firstNonEmpty(String... vals) {
        if (vals == null) return null;
        for (String v : vals) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

    /* ============================ DTOs ============================ */

    /** 프런트에 반환할 최소 필드 세트 */
    public static class DirectorMe {
        private String username;
        private String name;
        private String phone;
        private List<Integer> academyNumbers;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public List<Integer> getAcademyNumbers() { return academyNumbers; }
        public void setAcademyNumbers(List<Integer> academyNumbers) { this.academyNumbers = academyNumbers; }
    }

    /** 학원 요약 DTO */
    public static class AcademySummary {
        private Integer academyNumber;
        private String name;
        private String address;
        private String phone;

        public Integer getAcademyNumber() { return academyNumber; }
        public void setAcademyNumber(Integer academyNumber) { this.academyNumber = academyNumber; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
    }
}
