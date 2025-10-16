package com.rubypaper.controller;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import com.rubypaper.dto.BoardDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.rubypaper.domain.Answer;
import com.rubypaper.domain.Blacklist;
import com.rubypaper.domain.Board;
import com.rubypaper.domain.Member;
import com.rubypaper.service.BoardService;
import com.rubypaper.service.MemberService;
import com.rubypaper.persistence.AnswerRepository;
import com.rubypaper.persistence.BoardRepository;
import com.rubypaper.persistence.BlacklistRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class BoardController {

    @Autowired
    private BoardService boardService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private BlacklistRepository blacklistRepository;

    @Autowired
    private AnswerRepository answerRepository;
    
    
    // 게시글 목록 조회 (단, 차단 사용자 게시글 제외)
    @RequestMapping("/getBoardList")
    public String getBoardList(Board board, Model model, HttpSession session) {
        Member loginUser = (Member) session.getAttribute("user");

        // 차단된 사용자 ID 리스트
        List<String> blockedIds = (loginUser == null)
            ? List.of()
            : blacklistRepository.findByBlockerId(loginUser.getId())
                                 .stream()
                                 .map(Blacklist::getBlockedId)
                                 .collect(Collectors.toList());

        // 차단 사용자 글 제외
        List<Board> rawList = boardService.getBoardList(board)
            .stream()
            .filter(b -> b.getMember() != null
                      && !blockedIds.contains(b.getMember().getId()))
            .collect(Collectors.toList());

        // DTO 변환: 람다에 (Board b) 타입 명시
        List<BoardDto> boardDtos = rawList.stream()
            .map((Board b) -> new BoardDto(
                b.getSeq(),
                b.getTitle(),
                b.getMember().getId(),
                b.getCreateDate(),
                b.getAnswers().stream()
                 .anyMatch(a -> Boolean.TRUE.equals(a.isSelected()))
            ))
            .collect(Collectors.toList());

        model.addAttribute("boardList", boardDtos);
        return "getBoardList";
    }
    // 로그인 페이지 이동
    @RequestMapping("/loginview")
    public String loginview() {
        return "login";
    }

    // 새 글 등록 폼으로 이동
    @GetMapping("/insertBoard")
    public String insertBoardView() {
        return "insertBoard";
    }

    // 게시글 상세 조회
    @GetMapping("/getBoard")
    public String getBoard(@RequestParam("seq") Long seq, Model model, HttpSession session) {
        // 1) 게시글 정보
        Board board = boardService.getBoard(seq);
        model.addAttribute("board", board);

        // 2) 해당 게시글의 모든 답변
        List<Answer> answers = answerRepository.findByBoard_Seq(seq);
        model.addAttribute("answers", answers);

        // 3) 현재 로그인한 사용자
        Member loginUser = (Member) session.getAttribute("user");
        model.addAttribute("loginUser", loginUser);

        return "getBoard";
    }

    // 게시글 등록 처리
    @PostMapping("/insertBoard")
    public String insertBoard(Board board, HttpSession session,
                              @RequestParam(required = false) MultipartFile uploadfile) throws Exception {
        Member loginUser = (Member) session.getAttribute("user");
        if (loginUser != null) {
            board.setMember(loginUser);
        }

        // 파일 업로드 처리 (필요 시)
        if (uploadfile != null && !uploadfile.isEmpty()) {
            String filename = uploadfile.getOriginalFilename();
            uploadfile.transferTo(new File("c:/spring1/" + filename));
            board.setUploadFileName(filename);
        } else {
            board.setUploadFileName(null);
        }

        boardService.insertBoard(board);
        return "forward:getBoardList";
    }

    // 게시글 수정 폼 이동
    @GetMapping("/updateBoardView")
    public String updateBoardView(@RequestParam("seq") Long seq, Model model) {
        Board board = boardService.getBoard(seq);
        model.addAttribute("board", board);
        return "updateBoard";
    }

    // 게시글 수정 처리
    @PostMapping("/updateBoard")
    public String updateBoard(Board board) {
        boardService.updateBoard(board);
        return "redirect:getBoardList";
    }

    // 게시글 편집 이동 (alias)
    @GetMapping("/editBoard")
    public String editBoard(@RequestParam("seq") Long seq, Model model) {
        Board board = boardService.getBoard(seq);
        model.addAttribute("board", board);
        return "updateBoard";
    }
 // 게시글 삭제 처리
    @PostMapping("/deleteBoard")
    public String deleteBoard(@RequestParam("seq") Long seq, HttpSession session) {
        Member loginUser = (Member) session.getAttribute("user");
        if (loginUser == null) {
            return "redirect:/loginview";
        }

        Board board = boardService.getBoard(seq);
        if (!loginUser.getId().equals(board.getMember().getId())) {
            return "redirect:/getBoard?seq=" + seq;
        }

        // 기존 deleteBoard(Board) 호출
        boardService.deleteBoard(board);
        return "redirect:/getBoardList";
    }

    
}
