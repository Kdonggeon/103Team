package com.team103.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.team103.model.Parent;

public interface ParentRepository extends MongoRepository<Parent, String> {
    Parent findByUsernameAndPassword(String username, String password);
}