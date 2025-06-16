package com.mobile.greenacademypartner.model;

import com.google.gson.annotations.SerializedName;
import java.util.Date;

public class QaAnswer {

    private String id;

    @SerializedName("qa_id")  // JSON 통신 시 qa_id로 주고받음
    private String qaId;



    @SerializedName("content")
    private String content;

    @SerializedName("author_id")
    private String authorId;

    @SerializedName("author_role")
    private String authorRole;

    @SerializedName("created_at")
    private Date createdAt;

    @SerializedName("updated_at")
    private Date updatedAt;

    public QaAnswer() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getQaId() { return qaId; }
    public void setQaId(String qaId) { this.qaId = qaId; }

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
