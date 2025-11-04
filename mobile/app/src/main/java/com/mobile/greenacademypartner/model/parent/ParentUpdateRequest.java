package com.mobile.greenacademypartner.model.parent;

import com.google.gson.annotations.SerializedName;

public class ParentUpdateRequest {

    @SerializedName("Parents_Number")
    private String parentsId;

    @SerializedName("Parents_Name")
    private String parentsName;

    @SerializedName("Parents_Phone")
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
