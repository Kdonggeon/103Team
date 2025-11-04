package com.rubypaper.controller;

import com.rubypaper.domain.Answer;
import com.rubypaper.domain.Board;
import com.rubypaper.domain.Member;
import com.rubypaper.persistence.AnswerRepository;
import com.rubypaper.persistence.BoardRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/answer")
public class AnswerController {

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private BoardRepository boardRepository;

    @PostMapping("/add")
    public String addAnswer(@RequestParam Long boardId,
                            @RequestParam String content,
                            HttpSession session) {
        Member loginUser = (Member) session.getAttribute("user");
        if (loginUser == null) {
            return "redirect:/login";  // 로그인 없으면 login 화면으로 돌아가기
        }

        Optional<Board> optionalBoard = boardRepository.findById(boardId);
        if (optionalBoard.isEmpty()) {
            return "redirect:/getBoardList";  // board 없으면 목록으로 돌아가기
        }

        Board board = optionalBoard.get();
        Answer answer = new Answer();
        answer.setBoard(board);
        answer.setContent(content);
        answer.setWriterId(loginUser.getId());  // 작성자 id 저장
        answer.setAuthorId(loginUser.getId());  // author id 저장
        answer.setCreatedAt(LocalDateTime.now());  // 등록 시간 저장
        answer.setSelected(false);  // 초기에는 채택 안함

        answerRepository.save(answer);  // 답변 저장
        return "redirect:/getBoard?seq=" + boardId;  // 상세보기 화면으로 돌아가기
    }

    @PostMapping("/select/{id}")
    public String selectAnswer(@PathVariable Long id, HttpSession session) {
        Member loginUser = (Member) session.getAttribute("user");
        if (loginUser == null) {
            return "redirect:/login";  // 로그인 없으면 login 화면으로 돌아가기
        }

        Optional<Answer> optionalAnswer = answerRepository.findById(id);
        if (optionalAnswer.isEmpty()) {
            return "redirect:/getBoardList";  // answer 없으면 목록으로 돌아가기
        }

        Answer answer = optionalAnswer.get();
        Board board = answer.getBoard();

        List<Answer> allAnswers = answerRepository.findByBoard_Seq(board.getSeq());
        for (Answer a : allAnswers) {
            a.setSelected(false);  // 기존 채택 취소
        }

        answer.setSelected(true);  // 선택한 답변만 채택 표시
        answerRepository.saveAll(allAnswers);  // 변경된 답변 일괄 저장

        return "redirect:/getBoard?seq=" + board.getSeq();  // 상세보기 화면으로 돌아가기
    }
}
