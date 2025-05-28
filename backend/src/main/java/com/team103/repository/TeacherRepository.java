package com.team103.repository;

import com.team103.model.Teacher;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TeacherRepository extends MongoRepository<Teacher, String> {
    // 커스텀 쿼리도 여기에 추가 가능
}
