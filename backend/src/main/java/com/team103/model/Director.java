package com.team103.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Document(collection = "directors")  //
public class Director {

    @Id
    private String id;

    @Field("Director_Name")
    private String name;

    @Field("Director_ID")
    private String username;

    @Field("Director_PW")
    private String password;

    @Field("Director_Phone_Number")
    private String phone;

    @Field("Academy_Number")
    private List<Integer> academyNumbers;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public List<Integer> getAcademyNumbers() { return academyNumbers; }
    public void setAcademyNumbers(List<Integer> academyNumbers) {
        this.academyNumbers = academyNumbers;
    }
}
