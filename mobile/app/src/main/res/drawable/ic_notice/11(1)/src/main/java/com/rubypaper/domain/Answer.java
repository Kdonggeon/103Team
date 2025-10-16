package com.rubypaper.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity  // 이 클래스는 DB 테이블에 매핑됨
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // 기본 키 자동 생성
    private Long id;

    @ManyToOne  // 여러 답변이 하나의 게시글에 속함
    @JoinColumn(name = "BOARD_SEQ", nullable = false)
    private Board board;

    @Column(nullable = false, columnDefinition = "TEXT")  // 긴 텍스트 저장
    private String content;

    @Column(name = "WRITER_ID", nullable = false)  // 답변 작성자 ID
    private String writerId;

    private String authorId;  // 내부 로직용 ID

    private LocalDateTime createdAt;  // 생성 시각

    @Column(nullable = false)  // 채택 여부
    private boolean selected = false;

    // 필드 접근자
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public Board getBoard() {
        return board;
    }
    public void setBoard(Board board) {
        this.board = board;
    }

    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }

    public String getWriterId() {
        return writerId;
    }
    public void setWriterId(String writerId) {
        this.writerId = writerId;
    }

    public String getAuthorId() {
        return authorId;
    }
    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isSelected() {
        return selected;
    }
    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
