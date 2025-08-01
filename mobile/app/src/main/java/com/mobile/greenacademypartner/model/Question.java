package com.mobile.greenacademypartner.model;

public class Question {

    private String id;
    private String title;
    private String content;
    private String author;
    private String createdAt;
    private int academyNumber;

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
}
