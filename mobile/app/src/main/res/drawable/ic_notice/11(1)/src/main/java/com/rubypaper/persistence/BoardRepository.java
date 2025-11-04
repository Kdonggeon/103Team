package com.rubypaper.persistence;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.rubypaper.domain.Board;

public interface BoardRepository extends JpaRepository<Board, Long> {
	// 게시글의 기본키 타입은 long
}