package com.team103.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "students")
@Data // Getter, Setter, toString, equals, hashCode 자동 생성
@NoArgsConstructor // 기본 생성자 자동 생성
public class Student {

    @Id
    private String id; // MongoDB 고유 ID

    private String studentName;
    private long studentId;
    private String studentPw;
    private String studentAddress;
    private String studentPhoneNumber;
    private String school;
    private int grade;
    private String parentsNumber;
    private int seatNumber;
    private boolean checkedIn;
    private String gender;
}
