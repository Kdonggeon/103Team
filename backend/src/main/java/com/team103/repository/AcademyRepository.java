package com.team103.repository;

import com.team103.model.Academy;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AcademyRepository extends MongoRepository<Academy, String> {
    Academy findByName(String name); // 이름으로 조회하는 쿼리 예시
    Academy findByNumber(int number);

}
