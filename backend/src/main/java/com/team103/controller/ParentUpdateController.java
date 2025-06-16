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
    public ResponseEntity<?> updateParent(@PathVariable String id, @RequestBody ParentUpdateRequest request) {
        Parent parent = parentRepository.findByParentsId(id);
        if (parent == null) {
            return ResponseEntity.notFound().build();
        }

        parent.setParentsName(request.getParentsName());
        parent.setParentsPhoneNumber(request.getParentsPhoneNumber());

        parentRepository.save(parent);
        return ResponseEntity.ok("학부모 정보 수정 완료");
    }
}
