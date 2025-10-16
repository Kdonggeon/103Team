package com.rubypaper.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.rubypaper.domain.Answer;
import com.rubypaper.domain.Board;
import com.rubypaper.persistence.AnswerRepository;

@Service
public class AnswerService {

    @Autowired
    private AnswerRepository answerRepository;

    // 게시글의 모든 답변 가져오기
    public List<Answer> findByBoard(Board board) {
        return answerRepository.findByBoard_Seq(board.getSeq());
    }

    // 답변 저장
    public void saveAnswer(Answer answer) {
        answer.setCreatedAt(LocalDateTime.now());
        answerRepository.save(answer);
    }

    // 답변 채택 처리
    public void acceptAnswer(Long boardId, Long answerId) {
        List<Answer> allAnswers = answerRepository.findByBoard_Seq(boardId);
        for (Answer ans : allAnswers) {
            ans.setSelected(ans.getId().equals(answerId));
        }
        answerRepository.saveAll(allAnswers);
    }
}
