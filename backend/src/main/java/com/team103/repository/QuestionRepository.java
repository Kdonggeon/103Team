package com.team103.repository;

import com.team103.model.Question;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface QuestionRepository extends MongoRepository<Question, String> {
    List<Question> findByAuthor(String author);
    List<Question> findByAcademyNumber(int academyNumber);
}