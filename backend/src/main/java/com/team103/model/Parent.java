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

    // ✅ MongoDB의 필드명과 정확히 일치시켜야 함
    @Field("parentsId")
    private String parentsId;

    @Field("parentsPw")
    private String parentsPw;

    @Field("parentsName")
    private String parentsName;

    @Field("Parents_Phone_Number")
    private String parentsPhoneNumber;

    @Field("Parents_Number")
    private String parentsNumber;

    @Field("Student_ID_List")
    private List<String> studentIds;

    @Field("Academy_Numbers")
    private List<Integer> academyNumbers;

    public Parent() {}

    // ✅ 생성자 (필요 시 그대로 유지)
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

    // ✅ Getter / Setter
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

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    public List<Integer> getAcademyNumbers() { return academyNumbers; }
    public void setAcademyNumbers(List<Integer> academyNumbers) { this.academyNumbers = academyNumbers; }
}
