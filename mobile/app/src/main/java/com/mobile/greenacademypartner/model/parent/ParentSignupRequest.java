package com.mobile.greenacademypartner.model.parent;

public class ParentSignupRequest {
    private String parentsId;
    private String parentsPw;
    private String parentsName;
    private String parentsPhoneNumber;

    public ParentSignupRequest(String parentsId, String parentsPw, String parentsName, String parentsPhoneNumber) {
        this.parentsId = parentsId;
        this.parentsPw = parentsPw;
        this.parentsName = parentsName;
        this.parentsPhoneNumber = parentsPhoneNumber;
    }

    public String getParentsId() { return parentsId; }
    public String getParentsPw() { return parentsPw; }
    public String getParentsName() { return parentsName; }
    public String getParentsPhoneNumber() { return parentsPhoneNumber; }
}
