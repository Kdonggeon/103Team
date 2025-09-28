package com.team103.repository;

import com.team103.model.Director;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DirectorRepository extends MongoRepository<Director, String> {

    Director findByUsername(String username);
    boolean existsByUsername(String username);

    // 아이디 찾기용 (Director 엔티티에 필드명이 name, phone 일 때)
    Director findByNameAndPhone(String name, String phone);
}
