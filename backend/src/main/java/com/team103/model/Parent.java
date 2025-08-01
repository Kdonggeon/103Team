package com.team103.model;

import java.util.List;
import java.util.Collections;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "parents")
public class Parent {

    @Id
    private String id;

    private String fcmToken;
    private String parentsId;
    private String parentsPw;
    private String parentsName;

    @Field("Parents_Phone_Number")
    private String parentsPhoneNumber;

    @Field("Parents_Number")
    private String parentsNumber;

    // ✅ 여러 자녀 ID 등록 가능
    @Field("Student_ID_List")
    private List<String> studentIds;

    // ✅ 여러 학원 등록 가능
    @Field("Academy_Numbers")
    private List<Integer> academyNumbers;

    // ✅ 1. 회원가입 시 사용 (studentIds, academyNumbers 없이)
    public Parent(String parentsId, String parentsPw, String parentsName,
                  String parentsPhoneNumber, String parentsNumber, int academyNumber) {
        this.parentsId = parentsId;
        this.parentsPw = parentsPw;
        this.parentsName = parentsName;
        this.parentsPhoneNumber = parentsPhoneNumber;
        this.parentsNumber = parentsNumber;
        this.academyNumbers = Collections.singletonList(academyNumber);
    }

    // ✅ 2. 전체 필드 초기화용 (studentIds, academyNumbers 포함)
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

    // ✅ 3. 기본 생성자
    public Parent() {}

    // ✅ Getter/Setter
    public String getId() {
        return id;
    }

    public String getParentsId() {
        return parentsId;
    }

    public void setParentsId(String parentsId) {
        this.parentsId = parentsId;
    }

    public String getParentsPw() {
        return parentsPw;
    }

    public void setParentsPw(String parentsPw) {
        this.parentsPw = parentsPw;
    }

    public String getParentsName() {
        return parentsName;
    }

    public void setParentsName(String parentsName) {
        this.parentsName = parentsName;
    }

    public String getParentsPhoneNumber() {
        return parentsPhoneNumber;
    }

    public void setParentsPhoneNumber(String parentsPhoneNumber) {
        this.parentsPhoneNumber = parentsPhoneNumber;
    }

    public String getParentsNumber() {
        return parentsNumber;
    }

    public void setParentsNumber(String parentsNumber) {
        this.parentsNumber = parentsNumber;
    }

    public List<String> getStudentIds() {
        return studentIds;
    }

    public void setStudentIds(List<String> studentIds) {
        this.studentIds = studentIds;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public List<Integer> getAcademyNumbers() {
        return academyNumbers;
    }

    public void setAcademyNumbers(List<Integer> academyNumbers) {
        this.academyNumbers = academyNumbers;
    }
} 
