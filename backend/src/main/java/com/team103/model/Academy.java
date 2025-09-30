package com.team103.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "academy") // 컬렉션명이 'academy'가 맞는지 확인
public class Academy {

    @Id
    private String id;

    // DB 필드: academyNumber (정수)  ex) 103
    @Field("academyNumber")
    @Indexed(unique = true)
    private Integer academyNumber;

    // DB 필드: Academy_Name (문자열)  ex) "103학원"
    @Field("Academy_Name")
    private String name;

    @Field("Academy_Phone_Number")
    private String phone;

    @Field("Academy_Address")
    private String address;

    @Field("Director_Number")
    private Integer directorNumber;

    // --- getters / setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Integer getAcademyNumber() { return academyNumber; }
    public void setAcademyNumber(Integer academyNumber) { this.academyNumber = academyNumber; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Integer getDirectorNumber() { return directorNumber; }
    public void setDirectorNumber(Integer directorNumber) { this.directorNumber = directorNumber; }
}
