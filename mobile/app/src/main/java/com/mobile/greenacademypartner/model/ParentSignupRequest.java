package com.mobile.greenacademypartner.model;

public class ParentSignupRequest {
    private String parentsId;
    private String parentsPw;
    private String parentsName;
    private long parentsPhoneNumber;

    public ParentSignupRequest(String parentsId, String parentsPw, String parentsName, long parentsPhoneNumber) {
        this.parentsId = parentsId;
        this.parentsPw = parentsPw;
        this.parentsName = parentsName;
        this.parentsPhoneNumber = parentsPhoneNumber;
    }

    public String getParentsId() { return parentsId; }
    public String getParentsPw() { return parentsPw; }
    public String getParentsName() { return parentsName; }
    public long getParentsPhoneNumber() { return parentsPhoneNumber; }
}
