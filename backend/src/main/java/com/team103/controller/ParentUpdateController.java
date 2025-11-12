package com.team103.controller;

import com.team103.dto.ParentUpdateRequest;
import com.team103.model.Parent;
import com.team103.repository.ParentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/parents")
public class ParentUpdateController {

    @Autowired
    private ParentRepository parentRepository;

    @PutMapping("/{id}")
    public ResponseEntity<?> updateParent(
            @PathVariable String id,
            @RequestBody ParentUpdateRequest request) {

        try {
            // ✅ 1️⃣ 기존 문서 조회
            Parent parent = parentRepository.findByParentsId(id);
            if (parent == null) {
                return ResponseEntity.badRequest().body("❌ 해당 학부모를 찾을 수 없습니다: " + id);
            }

            // ✅ 2️⃣ 필요한 필드만 수정 (비밀번호, ID 등은 유지)
            boolean updated = false;

            if (request.getParentsName() != null && !request.getParentsName().isBlank()) {
                parent.setParentsName(request.getParentsName());
                updated = true;
            }
            if (request.getParentsPhoneNumber() != null && !request.getParentsPhoneNumber().isBlank()) {
                parent.setParentsPhoneNumber(request.getParentsPhoneNumber());
                updated = true;
            }

            if (!updated) {
                return ResponseEntity.badRequest().body("⚠️ 변경된 정보가 없습니다.");
            }

            // ✅ 3️⃣ 기존 문서 그대로 저장 (덮어쓰기 아님)
            Parent saved = parentRepository.save(parent);

            // ✅ 4️⃣ 수정된 전체 정보 그대로 반환
            return ResponseEntity.ok(saved);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("❌ 서버 오류: " + e.getMessage());
        }
    }
}
