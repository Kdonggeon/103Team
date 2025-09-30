package com.team103.repository;

import com.team103.model.Academy;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface AcademyRepository extends MongoRepository<Academy, String> {


    Academy findByAcademyNumber(Integer academyNumber);


    @Query("{ 'academyNumber' : ?0 }")
    Academy findByNumber(Integer number);


    Academy findByName(String name);
}
