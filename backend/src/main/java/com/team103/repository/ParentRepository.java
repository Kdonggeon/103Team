package com.team103.repository;

import com.team103.model.Parent;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ParentRepository extends MongoRepository<Parent, String> {
    boolean existsByUsername(String username); // 중복 확인용
    Parent findByUsername(String username);    // 로그인 시 사용
}
