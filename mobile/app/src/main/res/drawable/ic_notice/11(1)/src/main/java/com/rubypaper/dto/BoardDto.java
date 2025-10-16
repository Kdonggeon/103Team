package com.rubypaper.dto;

import java.util.Date;

public class BoardDto {
    private Long seq;          // 게시글 번호
    private String title;      // 게시글 제목
    private String writerId;   // 작성자 ID
    private Date createDate;   // 등록 일자
    private boolean hasSelected;  // 채택 여부

    public BoardDto(Long seq, String title, String writerId,
                    Date createDate, boolean hasSelected) {
        this.seq = seq;               // seq 세팅
        this.title = title;           // title 세팅
        this.writerId = writerId;     // writerId 세팅
        this.createDate = createDate; // createDate 세팅
        this.hasSelected = hasSelected; // hasSelected 세팅
    }

    public Long getSeq() { return seq; }             // seq 반환
    public void setSeq(Long seq) { this.seq = seq; } // seq 설정

    public String getTitle() { return title; }               // title 반환
    public void setTitle(String title) { this.title = title; } // title 설정

    public String getWriterId() { return writerId; }                     // writerId 반환
    public void setWriterId(String writerId) { this.writerId = writerId; } // writerId 설정

    public Date getCreateDate() { return createDate; }                     // createDate 반환
    public void setCreateDate(Date createDate) { this.createDate = createDate; } // createDate 설정

    public boolean isHasSelected() { return hasSelected; }                // hasSelected 반환
    public void setHasSelected(boolean hasSelected) { this.hasSelected = hasSelected; } // hasSelected 설정
}