package com.rubypaper.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rubypaper.domain.Answer;

public interface AnswerRepository extends JpaRepository<Answer, Long> {
    List<Answer> findByBoard_Seq(Long boardSeq);
    Answer findByBoard_SeqAndSelectedTrue(Long boardSeq);

    // 게시글(seq)에 달린 모든 답변을 삭제
    @Modifying
    @Query("DELETE FROM Answer a WHERE a.board.seq = :boardSeq")
    void deleteByBoardSeq(@Param("boardSeq") Long boardSeq);
}
