// backend/src/main/java/com/team103/controller/ParentManageController.java
package com.team103.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 학부모(Parent) 정보 수정 전용 컨트롤러.
 * - PATCH/PUT: /api/parents/{id}, /api/manage/teachers/parents/{id}
 * - change-id: /api/parents/{id}/change-id, /api/manage/teachers/parents/{id}/change-id
 *
 * 필드 동시 처리(케이스 혼용 지원):
 *  - 이름: parentsName / Parent_Name / parentName / name
 *  - 폰: parentsPhoneNumber / Parent_Phone_Number / phone / mobile
 *  - 학원번호: Academy_Numbers / academyNumbers / Academy_Number / academyNumber
 *  - 아이디 변경: newId / newParentsId / parentsId / Parent_ID / parentId / username / id
 */
@RestController
@CrossOrigin(origins = "*")
public class ParentManageController {

    private final MongoTemplate mongo;

    public ParentManageController(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    /** --------- 유틸 --------- */

    private Query parentIdQuery(String id) {
        return new Query(new Criteria().orOperator(
            Criteria.where("_id").is(id),
            Criteria.where("Parent_ID").is(id),
            Criteria.where("parentsId").is(id),
            Criteria.where("parentId").is(id),
            Criteria.where("username").is(id)
        ));
    }

    private static String pickStr(Map<String, Object> body, String... keys) {
        for (String k : keys) {
            Object v = body.get(k);
            if (v instanceof String s && !s.trim().isEmpty()) return s.trim();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static List<Integer> toIntList(Object raw) {
        if (raw == null) return null;
        List<Object> base;
        if (raw instanceof List<?> l) base = (List<Object>) l;
        else base = List.of(raw);
        List<Integer> out = new ArrayList<>();
        for (Object o : base) {
            try {
                if (o == null) continue;
                int n = Integer.parseInt(String.valueOf(o).trim());
                out.add(n);
            } catch (Exception ignore) {}
        }
        if (out.isEmpty()) return List.of();
        // 중복 제거
        return out.stream().distinct().collect(Collectors.toList());
    }

    /** --------- 공통 업데이트 로직 --------- */
    private ResponseEntity<?> doPatchParent(String id, Map<String, Object> body) {
        // 존재 확인
        Query q = parentIdQuery(id);
        var existing = mongo.findOne(q, Map.class, "parents"); // @Document(collection="parents") 가정
        if (existing == null) {
            // 컬렉션명이 다른 경우 어노테이션대로 동작하므로 타입 미지정 findOne도 시도
            existing = mongo.findOne(q, Map.class);
            if (existing == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PARENT_NOT_FOUND");
        }

        String name  = pickStr(body, "parentsName","Parent_Name","parentName","name");
        String phone = pickStr(body, "parentsPhoneNumber","Parent_Phone_Number","phone","mobile");

        Object rawAcademies = body.containsKey("Academy_Numbers") ? body.get("Academy_Numbers")
                             : body.containsKey("academyNumbers") ? body.get("academyNumbers")
                             : body.containsKey("Academy_Number") ? body.get("Academy_Number")
                             : body.get("academyNumber");
        List<Integer> academies = toIntList(rawAcademies);

        // PATCH 업데이트 쿼리
        Update u = new Update();
        if (name != null) {
            u.set("parentsName", name);
            u.set("Parent_Name", name);
            u.set("parentName", name);
            u.set("name", name);
        }
        if (phone != null) {
            u.set("parentsPhoneNumber", phone);
            u.set("Parent_Phone_Number", phone);
            u.set("phone", phone);
            u.set("mobile", phone);
        }
        if (academies != null) { // 전달됐을 때만 반영
            if (academies.size() <= 1) {
                Integer one = academies.isEmpty() ? null : academies.get(0);
                u.set("Academy_Number", one);
                u.set("academyNumber", one);
            }
            u.set("Academy_Numbers", academies);
            u.set("academyNumbers", academies);
        }

        // _id는 Mongo에서 변경 불가 → 여기서는 바꾸지 않음(아이디 변경은 change-id 엔드포인트 사용)
        mongo.updateFirst(q, u, "parents");

        // 최신 문서 반환
        var fresh = mongo.findOne(parentIdQuery(id), Map.class, "parents");
        if (fresh == null) {
            // 컬렉션명 다른 경우 대비
            fresh = mongo.findOne(parentIdQuery(id), Map.class);
        }
        return ResponseEntity.ok(Objects.requireNonNullElse(fresh, Map.of("ok", true, "id", id)));
    }

    /** --------- PATCH / PUT --------- */

    @PatchMapping({"/api/parents/{id}", "/api/manage/teachers/parents/{id}"})
    public ResponseEntity<?> patchParent(@PathVariable("id") String id,
                                         @RequestBody Map<String, Object> body) {
        // newId가 들어온 경우 이 엔드포인트에서는 일반 필드만 업데이트하고,
        // 실제 아이디 변경은 /change-id 로 처리(아래 POST 매핑)
        return doPatchParent(id, body);
    }

    @PutMapping({"/api/parents/{id}", "/api/manage/teachers/parents/{id}"})
    public ResponseEntity<?> putParent(@PathVariable("id") String id,
                                       @RequestBody Map<String, Object> body) {
        return doPatchParent(id, body);
    }

    /** --------- 아이디 변경 ---------
     * MongoDB는 _id 업데이트가 불가하여 새 문서 INSERT 후 기존 삭제 방식으로 처리.
     */
    @PostMapping({"/api/parents/{id}/change-id", "/api/manage/teachers/parents/{id}/change-id"})
    public ResponseEntity<?> changeParentId(@PathVariable("id") String id,
                                            @RequestBody Map<String, Object> body) {
        String newId = pickStr(body, "newId","newParentsId","parentsId","Parent_ID","parentId","username","id");
        if (newId == null || newId.equals(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "NEW_ID_REQUIRED");
        }

        Query qOld = parentIdQuery(id);
        Map oldDoc = mongo.findOne(qOld, Map.class, "parents");
        if (oldDoc == null) {
            oldDoc = mongo.findOne(qOld, Map.class);
            if (oldDoc == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PARENT_NOT_FOUND");
        }

        // 새 문서 구성
        Map<String,Object> newDoc = new LinkedHashMap<>(oldDoc);
        newDoc.put("_id", newId);
        newDoc.put("Parent_ID", newId);
        newDoc.put("parentsId", newId);
        newDoc.put("parentId", newId);
        newDoc.put("username", newId);

        // 충돌 체크
        Map exists = mongo.findById(newId, Map.class, "parents");
        if (exists != null) throw new ResponseStatusException(HttpStatus.CONFLICT, "NEW_ID_ALREADY_EXISTS");

        // insert → delete 순
        mongo.insert(newDoc, "parents");
        mongo.remove(qOld, "parents");

        // 새 문서 반환
        Map fresh = mongo.findById(newId, Map.class, "parents");
        return ResponseEntity.ok(Objects.requireNonNullElse(fresh, Map.of("ok", true, "id", newId)));
    }
}
