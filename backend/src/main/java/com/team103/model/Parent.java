package com.team103.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "parents")
public class Parent {

    @Id
    private String id;

    @Field("Parents_ID")
    private String parentsId;

    @Field("Parents_PW")
    private String parentsPw;

    @Field("Parents_Name")
    private String parentsName;

    @Field("Parents_Phone_Number")
    private long parentsPhoneNumber;

    public Parent(String id, String parentsId, String parentsPw, String parentsName, long parentsPhoneNumber) {
        this.id = id;
        this.parentsId = parentsId;
        this.parentsPw = parentsPw;
        this.parentsName = parentsName;
        this.parentsPhoneNumber = parentsPhoneNumber;
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
