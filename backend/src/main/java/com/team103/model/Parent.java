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

    // 소문자 필드 (신규 규칙)
    private String parentsId;
    private String parentsPw;
    private String parentsName;

    // ✅ 레거시 대문자 키도 함께 매핑 (읽기용 폴백)
    @Field("Parents_ID")
    private String parentsIdLegacy;

    @Field("Parents_Name")
    private String parentsNameLegacy;

    @Field("Parents_Phone_Number")
    private String parentsPhoneNumber;

    @Field("Parents_Number")
    private String parentsNumber;

    // 여러 자녀 ID
    @Field("Student_ID_List")
    private List<String> studentIds;

    // 여러 학원 번호
    @Field("Academy_Numbers")
    private List<Integer> academyNumbers;

    // --- 생성자들 ---
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

    public Parent() {}

    // --- Getter/Setter ---

    public String getId() { return id; }

    /** ✅ ID 폴백: parentsId → Parents_ID(레거시) */
    public String getParentsId() {
        String v = trimOrNull(parentsId);
        if (v != null) return v;
        return trimOrNull(parentsIdLegacy);
    }

    public void setParentsId(String parentsId) { this.parentsId = parentsId; }

    public String getParentsPw() { return parentsPw; }
    public void setParentsPw(String parentsPw) { this.parentsPw = parentsPw; }

    /** ✅ 이름 폴백: parentsName(소문자) → Parents_Name(대문자) → parentsId */
    public String getParentsName() {
        String v = trimOrNull(parentsName);
        if (v != null) return v;
        v = trimOrNull(parentsNameLegacy);
        if (v != null) return v;
        // 최후 폴백: ID
        return getParentsId();
    }

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

    // --- 내부 유틸 ---
    private static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
