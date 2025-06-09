package com.team103.dto;

import com.team103.model.Parent;

public class ParentSignupRequest {

    private String id;
    private String parentsId;
    private String parentsPw;
    private String parentsName;
    private long parentsPhoneNumber;

    public Parent toEntity(String encodedPw) {
        return new Parent(id, parentsId, encodedPw, parentsName, parentsPhoneNumber);
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getParentsId() { return parentsId; }
    public void setParentsId(String parentsId) { this.parentsId = parentsId; }

    public String getParentsPw() { return parentsPw; }
    public void setParentsPw(String parentsPw) { this.parentsPw = parentsPw; }

    public String getParentsName() { return parentsName; }
    public void setParentsName(String parentsName) { this.parentsName = parentsName; }

    public long getParentsPhoneNumber() { return parentsPhoneNumber; }
    public void setParentsPhoneNumber(long parentsPhoneNumber) { this.parentsPhoneNumber = parentsPhoneNumber; }
}
