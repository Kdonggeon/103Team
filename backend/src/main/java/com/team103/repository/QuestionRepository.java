package com.team103.repository;

import com.team103.model.Question;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.Query;

public interface QuestionRepository extends MongoRepository<Question, String> {
    List<Question> findByAuthor(String author);
    List<Question> findByAcademyNumber(int academyNumber);
    Optional<Question> findFirstByAcademyNumberAndRoomTrue(int academyNumber);
    @Query("{ 'room': true, 'academyNumber': ?0, 'roomStudentId': ?1 }")
    Question findRoomByAcademyAndStudent(int academyNumber, String roomStudentId);
}