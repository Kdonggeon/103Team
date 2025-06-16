package com.mobile.greenacademypartner.model;

import com.google.gson.annotations.SerializedName;
import java.util.Date;
import java.io.Serializable;

/**
 * QA 모델 (Android 클라이언트용)
 * Notice 모델과 동일한 형식으로 작성됨
 */
public class Qa implements Serializable {
    private static final long serialVersionUID = 1L;

    @SerializedName("id")
    private String id;

    @SerializedName("author")
    private String author;

    @SerializedName("title")
    private String title;

    @SerializedName("content")
    private String content;

    /**
     * 서버에서 내려주는 'answer' JSON 오브젝트 전체를 매핑
     */
    @SerializedName("answer")
    private Answer answer;

    @SerializedName("created_at")
    private Date createdAt;

    // 기본 생성자
    public Qa() {}

    // getters & setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Answer getAnswer() { return answer; }
    public void setAnswer(Answer answer) { this.answer = answer; }

    /**
     * Answer.authorRole을 바로 가져오는 편의 메서드
     */
    public String getAuthorRole() {
        return answer != null ? answer.getAuthorRole() : null;
    }
    public void setAuthorRole(String authorRole) {
        if (answer == null) answer = new Answer();
        answer.setAuthorRole(authorRole);
    }

    public String getAuthorId() {
        return answer != null ? answer.getAuthorId() : null;
    }
    public void setAuthorId(String authorId) {
        if (answer == null) answer = new Answer();
        answer.setAuthorId(authorId);
    }

    public java.util.List<Answer> getAnswers() {
        java.util.List<Answer> list = new java.util.ArrayList<>();
        if (answer != null) list.add(answer);
        return list;
    }

    // Nested Answer class
    public static class Answer implements Serializable {
        private static final long serialVersionUID = 1L;

        @SerializedName("_id")
        private String id;

        @SerializedName("content")
        private String content;

        @SerializedName("authorId")
        private String authorId;

        @SerializedName("authorRole")
        private String authorRole;

        @SerializedName("created_at")
        private Date createdAt;

        @SerializedName("updated_at")
        private Date updatedAt;

        public Answer() {}

        // getters & setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getAuthorId() { return authorId; }
        public void setAuthorId(String authorId) { this.authorId = authorId; }

        public String getAuthorRole() { return authorRole; }
        public void setAuthorRole(String authorRole) { this.authorRole = authorRole; }

        public Date getCreatedAt() { return createdAt; }
        public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

        public Date getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    }
}