package com.team103.repository;

import com.team103.model.Director;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DirectorRepository extends MongoRepository<Director, String> {
	Director findByUsername(String username);
	boolean existsByUsername(String username);
}