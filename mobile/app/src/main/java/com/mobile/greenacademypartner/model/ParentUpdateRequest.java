package com.mobile.greenacademypartner.model;

public class ParentUpdateRequest {
    private String parentsId;
    private String parentsName;
    private String parentsPhoneNumber;

    public ParentUpdateRequest() {}

    public ParentUpdateRequest(String parentsId, String parentsName, String parentsPhoneNumber) {
        this.parentsId = parentsId;
        this.parentsName = parentsName;
        this.parentsPhoneNumber = parentsPhoneNumber;
    }

    public String getParentsId() { return parentsId; }
    public String getParentsName() { return parentsName; }
    public String getParentsPhoneNumber() { return parentsPhoneNumber; }

    public void setParentsId(String parentsId) { this.parentsId = parentsId; }
    public void setParentsName(String parentsName) { this.parentsName = parentsName; }
    public void setParentsPhoneNumber(String parentsPhoneNumber) { this.parentsPhoneNumber = parentsPhoneNumber; }
}
