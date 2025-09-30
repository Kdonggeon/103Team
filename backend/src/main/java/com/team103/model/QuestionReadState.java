package com.team103.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Document(collection = "question_read_state")
public class QuestionReadState {
    @Id private String id;
    private String questionId;
    private String userId;
    private Date lastReadAt;

    public String getId(){ return id; } public void setId(String id){ this.id = id; }
    public String getQuestionId(){ return questionId; } public void setQuestionId(String v){ this.questionId = v; }
    public String getUserId(){ return userId; } public void setUserId(String v){ this.userId = v; }
    public Date getLastReadAt(){ return lastReadAt; } public void setLastReadAt(Date v){ this.lastReadAt = v; }
}