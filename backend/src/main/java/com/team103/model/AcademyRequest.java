package com.team103.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * 학원 연결 승인 요청 (학생/학부모/교사 → 원장).
 */
@Document(collection = "academy_requests")
public class AcademyRequest {

    @Id
    private String id;

    private Integer academyNumber;
    private String requesterId;     // studentId / parentId / teacherId
    private String requesterRole;   // student | parent | teacher

    private String memo;            // 요청 사유(선택)

    private String status;          // PENDING / APPROVED / REJECTED
    private Date createdAt;
    private Date updatedAt;

    private String processedBy;     // director username
    private String processedMemo;   // 승인/거절 코멘트

    public AcademyRequest() {
        this.status = "PENDING";
        Date now = new Date();
        this.createdAt = now;
        this.updatedAt = now;
    }

    // getters/setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Integer getAcademyNumber() { return academyNumber; }
    public void setAcademyNumber(Integer academyNumber) { this.academyNumber = academyNumber; }

    public String getRequesterId() { return requesterId; }
    public void setRequesterId(String requesterId) { this.requesterId = requesterId; }

    public String getRequesterRole() { return requesterRole; }
    public void setRequesterRole(String requesterRole) { this.requesterRole = requesterRole; }

    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public String getProcessedBy() { return processedBy; }
    public void setProcessedBy(String processedBy) { this.processedBy = processedBy; }

    public String getProcessedMemo() { return processedMemo; }
    public void setProcessedMemo(String processedMemo) { this.processedMemo = processedMemo; }
}
