package com.team103.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "students")
@Data
@NoArgsConstructor
public class Student {

    @Id
    private String id;

    @Field("Student_Name")
    private String studentName;

    @Field("Student_ID")
    private long studentId;

    @Field("Student_PW")
    private String studentPw;

    @Field("Student_Address")
    private String studentAddress;

    @Field("Student_Phone_Number")
    private String studentPhoneNumber;

    @Field("School")
    private String school;

    @Field("Grade")
    private int grade;

    @Field("Parents_Number")
    private String parentsNumber;

    @Field("Seat_Number")
    private int seatNumber;

    @Field("Checked_In")
    private boolean checkedIn;

    @Field("Gender")
    private String gender;

    public String getUsername() {
        return String.valueOf(studentId);
    }

    public String getName() {
        return studentName;
    }
}
