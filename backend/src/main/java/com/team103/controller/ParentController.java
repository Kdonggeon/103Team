package com.team103.controller;

import com.team103.model.Parent;
import com.team103.repository.ParentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parents")
public class ParentController {

    @Autowired
    private ParentRepository parentRepository;

    // ✅ 전체 부모 목록 조회
    @GetMapping
    public List<Parent> getAllParents() {
        return parentRepository.findAll();
    }

    // ✅ ID로 부모 조회
    @GetMapping("/{id}")
    public Parent getParentById(@PathVariable String id) {
        return parentRepository.findById(id).orElse(null);
    }

    // ✅ 부모 등록 (테스트용)
    @PostMapping
    public Parent createParent(@RequestBody Parent parent) {
        return parentRepository.save(parent);
    }

    // ✅ 부모 정보 수정
    @PutMapping("/{id}")
    public Parent updateParent(@PathVariable String id, @RequestBody Parent updated) {
        return parentRepository.findById(id)
            .map(parent -> {
                parent.setName(updated.getName());
                parent.setUsername(updated.getUsername());
                parent.setPassword(updated.getPassword());
                return parentRepository.save(parent);
            }).orElse(null);
    }

    // ✅ 부모 삭제
    @DeleteMapping("/{id}")
    public void deleteParent(@PathVariable String id) {
        parentRepository.deleteById(id);
    }
}
