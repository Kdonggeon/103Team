package com.team103.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Document(collection = "teachers")
public class Teacher {

    @Id
    private String id;

    @Field("Teacher_ID")
    private String username;  // 로그인용 ID

    @Field("Teacher_PW")
    private String password;  // 로그인용 비밀번호

    @Field("Teacher_Name")
    private String name;

    @Field("Teacher_Phone_Number")
    private String phoneNumber;

    @Field("Academy_Number")
    private String academyNumber;
    
    public String getUsername() {
        return String.valueOf(username);
    }

    public String getName() {
        return name;
    }
}
