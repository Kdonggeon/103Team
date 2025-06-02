package com.team103.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.Data;
@Data
@Document(collection = "parents")
public class Parent {

    @Id
    private String id;

    @Field("Parents_ID")
    private String username;

    @Field("Parents_PW")
    private String password;

    @Field("Parents_Name")
    private String name;

    @Field("Parents_Phone_Number")
    private String phoneNumber;

    // 기본 생성자
    public Parent() {}

    public Parent(String username, String password, String name) {
        this.username = username;
        this.password = password;
        this.name = name;
    }

    // Getter/Setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    

}
