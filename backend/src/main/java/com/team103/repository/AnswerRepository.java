package com.team103.repository;

import com.team103.model.Answer;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface AnswerRepository extends MongoRepository<Answer, String> {
    List<Answer> findByQuestionId(String questionId);
    void deleteByQuestionId(String questionId);
}
