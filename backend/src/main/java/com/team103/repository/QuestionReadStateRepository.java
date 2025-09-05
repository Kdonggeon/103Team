package com.team103.repository;

import com.team103.model.QuestionReadState;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface QuestionReadStateRepository extends MongoRepository<QuestionReadState, String> {
    Optional<QuestionReadState> findByQuestionIdAndUserId(String questionId, String userId);
}