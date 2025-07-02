package com.team103.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "parents")
public class Parent {

    @Id
    private String id;

    private String parentsId;
    private String parentsPw;
    private String parentsName;
    @Field("Parents_Phone_Number")
    private String parentsPhoneNumber;

    @Field("Parents_Number")
    private String parentsNumber;



    



    public Parent(String parentsId, String parentsPw, String parentsName, String parentsPhoneNumber, String parentsNumber) {

        this.parentsId = parentsId;
        this.parentsPw = parentsPw;
        this.parentsName = parentsName;
        this.parentsPhoneNumber = parentsPhoneNumber;
        this.parentsNumber = parentsNumber;

    
    }

    public String getId() { return id; }

    public String getParentsId() { return parentsId; }
    public void setParentsId(String parentsId) { this.parentsId = parentsId; }

    public String getParentsPw() { return parentsPw; }
    public void setParentsPw(String parentsPw) { this.parentsPw = parentsPw; }

    public String getParentsName() { return parentsName; }
    public void setParentsName(String parentsName) { this.parentsName = parentsName; }

    public String getParentsPhoneNumber() { return parentsPhoneNumber; }
    public void setParentsPhoneNumber(String parentsPhoneNumber) { this.parentsPhoneNumber = parentsPhoneNumber; }
    
    public String getParentsNumber() {
		return parentsNumber;
	}

	public void setParentsNumber(String parentsNumber) {
		this.parentsNumber = parentsNumber;
	}
	
}
