package com.team103.repository;

import com.team103.model.Parent;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ParentRepository extends MongoRepository<Parent, String> {
    boolean existsByParentsId(String parentsId);
    Parent findByParentsId(String parentsId);
    
    Parent findByParentsNameAndParentsPhoneNumber(String name, String phone);

    }

