package com.team103.repository;

import com.team103.model.Parent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

import java.util.List;

public interface ParentRepository extends MongoRepository<Parent, String> {
    boolean existsByParentsId(String parentsId);
    Parent findByParentsId(String parentsId);
    Parent findByParentsNameAndParentsPhoneNumber(String name, String phone);
    boolean existsByParentsNumber(String parentsNumber);

    // ✅ 자녀ID를 포함한 학부모 전체 조회
    @Query("{ 'Student_ID_List': ?0 }")
    List<Parent> findByStudentId(String studentId);

    // ✅ (이미 쓰고 있던) 모든 학부모에서 해당 자녀ID 제거
    @Query(value = "{ 'Student_ID_List': ?0 }")
    @Update(value = "{ '$pull': { 'Student_ID_List': ?0 } }")
    void pullStudentIdFromAll(String studentId);
}
