package com.team103.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.team103.model.Student;

public interface StudentRepository extends MongoRepository<Student, String> {
	Student findByStudentIdAndStudentPw(long studentId, int studentPw);


	
	Student findByStudentId(long studentId);


}
