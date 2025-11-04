package com.rubypaper.service;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.rubypaper.domain.Board;
import com.rubypaper.persistence.AnswerRepository;
import com.rubypaper.persistence.BoardRepository;

import jakarta.transaction.Transactional;

@Service
@Transactional  
public class BoardServiceImpl implements BoardService{
	@Autowired
	private BoardRepository boardRepository;
	
	@Override                 
	public List<Board> getBoardList(Board board){
		List<Board>  listboard=boardRepository.findAll();
		return listboard;
	}
    @Override
	public void insertBoard(Board board){// 글 등록 처리
		boardRepository.save(board);    	}
    
    
    @Override //주어진 게시글 번호(seq)에 해당하는 게시글이 있으면 반환함
    public Board getBoard(Long seq) { 
        return boardRepository.findById(seq).orElse(null);
    }
    
    
	@Override
	public void updateBoard(Board board){  // 글수정
		Board findBoard 
		     =boardRepository.findById(board.getSeq()).get();  
		
		findBoard.setTitle(board.getTitle());
		findBoard.setContent(board.getContent());
		
		boardRepository.save(findBoard);                        
	}	

	@Autowired
    private AnswerRepository answerRepository;
	
    @Override
    public void deleteBoard(Board board) {
        // 1) 답변 먼저 삭제
        answerRepository.deleteByBoardSeq(board.getSeq());
        // 2) 게시글 삭제
        boardRepository.deleteById(board.getSeq());
    }
	
  }






