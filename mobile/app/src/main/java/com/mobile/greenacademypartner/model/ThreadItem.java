package com.mobile.greenacademypartner.model;

public class ThreadItem {

    public enum Type { QUESTION, ANSWER }

    private Type type;
    private String id;
    private String content;

    // 작성자 정보
    private String author;      // 로그인 ID
    private String authorName;  // 선택(서버가 보내줄 경우 사용)

    private String createdAt;

    public ThreadItem() { }

    // ====== 팩토리 메서드: Question -> ThreadItem ======
    public static ThreadItem fromQuestion(Question q) {
        ThreadItem t = new ThreadItem();
        t.type = Type.QUESTION;
        t.id = q.getId();
        t.content = q.getContent();
        t.author = q.getAuthor();
        t.authorName = q.getAuthorName();  // 서버가 주면 사용, 아니면 null
        t.createdAt = q.getCreatedAt();
        return t;
    }

    // ====== 팩토리 메서드: Answer -> ThreadItem ======
    public static ThreadItem fromAnswer(Answer a) {
        ThreadItem t = new ThreadItem();
        t.type = Type.ANSWER;
        t.id = a.getId();
        t.content = a.getContent();
        t.author = a.getAuthor();
        t.authorName = a.getAuthorName();  // 서버가 주면 사용, 아니면 null
        t.createdAt = a.getCreatedAt();
        return t;
    }

    // ====== Getter / Setter ======
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
