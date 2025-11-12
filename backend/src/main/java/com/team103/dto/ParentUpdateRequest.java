package com.team103.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ParentUpdateRequest {

    @JsonProperty("Parents_Number")
    private String parentsId;

    @JsonProperty("Parents_Name")
    private String parentsName;

    @JsonProperty("Parents_Phone")
    private String parentsPhoneNumber;

    public ParentUpdateRequest() {}

    public String getParentsId() { return parentsId; }
    public void setParentsId(String parentsId) { this.parentsId = parentsId; }

    public String getParentsName() { return parentsName; }
    public void setParentsName(String parentsName) { this.parentsName = parentsName; }

    public String getParentsPhoneNumber() { return parentsPhoneNumber; }
    public void setParentsPhoneNumber(String parentsPhoneNumber) { this.parentsPhoneNumber = parentsPhoneNumber; }
}
