package com.team103.dto;

public class QnaRecentResponse {
    private String questionId;
    private String answerId; // 없을 수 있음
    private String source;   // "ANSWER" | "QUESTION"

    public QnaRecentResponse() {}
    public QnaRecentResponse(String questionId, String answerId, String source) {
        this.questionId = questionId;
        this.answerId = answerId;
        this.source = source;
    }

    public String getQuestionId() { return questionId; }
    public void setQuestionId(String questionId) { this.questionId = questionId; }
    public String getAnswerId() { return answerId; }
    public void setAnswerId(String answerId) { this.answerId = answerId; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
