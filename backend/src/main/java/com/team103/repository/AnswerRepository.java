package com.team103.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.team103.model.Answer;

import java.util.List;

public interface AnswerRepository extends MongoRepository<Answer, String> {
    List<Answer> findByQuestionId(String questionId);
    void deleteByQuestionId(String questionId);
    List<Answer> findByQuestionIdAndDeletedFalse(String questionId);

    // deleted=false 이거나 deleted 필드가 아예 없는 문서까지 포함
    @Query(value = "{ 'questionId': ?0, $or: [ { 'deleted': false }, { 'deleted': { $exists: false } } ] }")
    List<Answer> findActiveByQuestionId(String questionId);


    @Query(value = "{ 'questionId': ?0, $or: [ { 'deleted': false }, { 'deleted': { $exists: false } } ] }",
           sort  = "{ 'createdAt': -1 }")
    Answer findLatestActiveByQuestionId(String questionId);
    // 특정 질문들에 달린 최신 답변 1건 (deleted=false)
    Answer findTopByQuestionIdInAndDeletedFalseOrderByCreatedAtDesc(List<String> questionIds);

    // 내가 작성한 최신 답변 1건 (교사/원장 1순위)
    Answer findTopByAuthorOrderByCreatedAtDesc(String author);

    // (참고) 단일 질문의 최신 답변
    Answer findTopByQuestionIdAndDeletedFalseOrderByCreatedAtDesc(String questionId);
}
