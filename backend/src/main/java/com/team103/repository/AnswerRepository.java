package com.team103.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.team103.model.Answer;

import java.util.List;

public interface AnswerRepository extends MongoRepository<Answer, String> {

    List<Answer> findByQuestionId(String questionId);

    void deleteByQuestionId(String questionId);

    List<Answer> findByQuestionIdAndDeletedFalse(String questionId);

    // deleted=false 이거나 삭제 필드가 없는 문서 포함
    @Query(value = "{ 'questionId': ?0, $or: [ { 'deleted': false }, { 'deleted': { $exists: false } } ] }")
    List<Answer> findActiveByQuestionId(String questionId);

    @Query(
            value = "{ 'questionId': ?0, $or: [ { 'deleted': false }, { 'deleted': { $exists: false } } ] }",
            sort  = "{ 'createdAt': -1 }"
    )
    Answer findLatestActiveByQuestionId(String questionId);

    // 질문 여러 개 중 최신 1건
    Answer findTopByQuestionIdInAndDeletedFalseOrderByCreatedAtDesc(List<String> questionIds);

    // 내가 작성한 최신 답변 1건
    Answer findTopByAuthorOrderByCreatedAtDesc(String author);

    // 단일 질문의 최신 답변
    Answer findTopByQuestionIdAndDeletedFalseOrderByCreatedAtDesc(String questionId);

    // 🔥 전체 답변에서 최신 n개
    @Query(
        value = "{ $or: [ { 'deleted': false }, { 'deleted': { $exists: false } } ] }",
        sort = "{ 'createdAt': -1 }"
    )
    List<Answer> findRecentActiveAnswers(org.springframework.data.domain.Pageable pageable);

    // 🔥🔥 내 방(questionIds list)에서 최신 n개 답변 (핵심)
    @Query(
        value = "{ 'questionId': { $in: ?0 }, $or: [ { 'deleted': false }, { 'deleted': { $exists: false } } ] }",
        sort = "{ 'createdAt': -1 }"
    )
    List<Answer> findByQuestionIdInAndDeletedFalseOrderByCreatedAtDesc(List<String> questionIds);

}
