package com.team103.repository;

import com.team103.model.Parent;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ParentRepository extends MongoRepository<Parent, String> {
	boolean existsByParentsId(String parentsId);
    Parent findByParentsId(String parentsId);
    Parent findByParentsNameAndParentsPhoneNumber(String name, String phone);

    // ✅ 추가: Parents_Number 중복 방지용
    boolean existsByParentsNumber(String parentsNumber);

    }

