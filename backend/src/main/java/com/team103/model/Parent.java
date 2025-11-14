package com.team103.model;

import java.util.List;
import java.util.Collections;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.fasterxml.jackson.annotation.JsonProperty;

@Document(collection = "parents")
public class Parent {

    @Id
    private String id;

    private String fcmToken;

    // ============================================================
    //  기본 계정 정보
    // ============================================================

    @Field("parentsId")
    @JsonProperty("parentsId")
    private String parentsId;

    @Field("parentsPw")
    @JsonProperty("parentsPw")
    private String parentsPw;

    @Field("parentsName")
    @JsonProperty("parentsName")
    private String parentsName;

    @Field("Parents_Phone_Number")
    @JsonProperty("parentsPhoneNumber")
    private String parentsPhoneNumber;

    @Field("Parents_Number")
    @JsonProperty("parentsNumber")
    private String parentsNumber;


    // ============================================================
    //  연관 정보 (여기가 문제였던 핵심)
    // ============================================================

    // 자녀 목록 → JSON에서는 childStudentId 로 내려가야 함
    @Field("Student_ID_List")
    @JsonProperty("childStudentId")
    private List<String> studentIds;

    // 학원 목록 → JSON에서는 academyNumbers 로 내려가야 함
    @Field("Academy_Numbers")
    @JsonProperty("academyNumbers")
    private List<Integer> academyNumbers;


    // ============================================================
    //  생성자들
    // ============================================================

    public Parent() {}

    public Parent(String parentsId, String parentsPw, String parentsName,
                  String parentsPhoneNumber, String parentsNumber, int academyNumber) {
        this.parentsId = parentsId;
        this.parentsPw = parentsPw;
        this.parentsName = parentsName;
        this.parentsPhoneNumber = parentsPhoneNumber;
        this.parentsNumber = parentsNumber;
        this.academyNumbers = Collections.singletonList(academyNumber);
    }

    public Parent(String parentsId, String parentsPw, String parentsName,
                  String parentsPhoneNumber, String parentsNumber,
                  List<String> studentIds, List<Integer> academyNumbers) {
        this.parentsId = parentsId;
        this.parentsPw = parentsPw;
        this.parentsName = parentsName;
        this.parentsPhoneNumber = parentsPhoneNumber;
        this.parentsNumber = parentsNumber;
        this.studentIds = studentIds;
        this.academyNumbers = academyNumbers;
    }


    // ============================================================
    //  Getter / Setter
    // ============================================================

    public String getId() { return id; }

    public String getParentsId() { return parentsId; }
    public void setParentsId(String parentsId) { this.parentsId = parentsId; }

    public String getParentsPw() { return parentsPw; }
    public void setParentsPw(String parentsPw) { this.parentsPw = parentsPw; }

    public String getParentsName() { return parentsName; }
    public void setParentsName(String parentsName) { this.parentsName = parentsName; }

    public String getParentsPhoneNumber() { return parentsPhoneNumber; }
    public void setParentsPhoneNumber(String parentsPhoneNumber) { this.parentsPhoneNumber = parentsPhoneNumber; }

    public String getParentsNumber() { return parentsNumber; }
    public void setParentsNumber(String parentsNumber) { this.parentsNumber = parentsNumber; }

    public List<String> getStudentIds() { return studentIds; }
    public void setStudentIds(List<String> studentIds) { this.studentIds = studentIds; }

    public List<Integer> getAcademyNumbers() { return academyNumbers; }
    public void setAcademyNumbers(List<Integer> academyNumbers) { this.academyNumbers = academyNumbers; }

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
}
