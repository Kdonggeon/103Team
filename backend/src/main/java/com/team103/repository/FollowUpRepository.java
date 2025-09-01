package com.team103.repository;

import com.team103.model.FollowUp;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface FollowUpRepository extends MongoRepository<FollowUp, String> {
    List<FollowUp> findByQuestionIdAndDeletedFalse(String questionId);
}
