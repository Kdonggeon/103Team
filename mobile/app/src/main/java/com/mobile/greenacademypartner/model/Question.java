package com.mobile.greenacademypartner.model;

public class Question {

    private String id;
    private String title;
    private String content;
    private String author;
    private String createdAt;
    private int academyNumber;

    // ✅ 이름 표시용(선택): 서버가 내려주면 사용, 없으면 null
    private String authorName;

    public Question() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public int getAcademyNumber() {
        return academyNumber;
    }

    public void setAcademyNumber(int academyNumber) {
        this.academyNumber = academyNumber;
    }

    // ====== 추가: authorName ======
    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }
}
