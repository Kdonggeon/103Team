package com.team103.repository;

import com.team103.model.Teacher;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TeacherRepository extends MongoRepository<Teacher, String> {
	Teacher findByUsernameAndPassword(String username, String password);;
}

