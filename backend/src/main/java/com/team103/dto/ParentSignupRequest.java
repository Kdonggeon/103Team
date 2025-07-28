package com.team103.dto;

import com.team103.model.Parent;

public class ParentSignupRequest {

    private String id;
    private String parentsId;
    private String parentsPw;
    private String parentsName;
    private String parentsPhoneNumber;
    private String parentsNumber;
    private int academyNumber;

    // 🔽 toEntity 메서드 정의
    public String getParentsNumber() { return parentsNumber; }
    public void setParentsNumber(String parentsNumber) { this.parentsNumber = parentsNumber; }

    public Parent toEntity(String encodedPw) {
        return new Parent(
            parentsId,
            encodedPw,
            parentsName,
            parentsPhoneNumber,
            parentsNumber,
            academyNumber
        );
    }

    // Getter/Setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getParentsId() { return parentsId; }
    public void setParentsId(String parentsId) { this.parentsId = parentsId; }

    public String getParentsPw() { return parentsPw; }
    public void setParentsPw(String parentsPw) { this.parentsPw = parentsPw; }

    public String getParentsName() { return parentsName; }
    public void setParentsName(String parentsName) { this.parentsName = parentsName; }

    public String getParentsPhoneNumber() { return parentsPhoneNumber; }
    public void setParentsPhoneNumber(String parentsPhoneNumber) { this.parentsPhoneNumber = parentsPhoneNumber; }
    
    public int getAcademyNumber() {
        return academyNumber;
    }

    public void setAcademyNumber(int academyNumber) {
        this.academyNumber = academyNumber;
    }
}
