package com.team103.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 아이디 찾기 요청 DTO
 * - 기본 키: name, phoneNumber
 * - 호환 키: phone, studentPhoneNumber, Parents_Phone_Number, Teacher_Phone_Number 등
 * - 역할별 분리(학생/학부모/교사) 컨트롤러에서 공통으로 사용 가능
 */
public class FindIdRequest {

    /** 통합 엔드포인트(/api/find_id)에서만 사용. 역할별 분리 구조라면 없어도 무방 */
    @JsonAlias({"role", "Role"})
    private String role;

    @JsonProperty("name")
    @JsonAlias({"Name", "studentName", "Parents_Name", "Teacher_Name", "Director_Name"})
    private String name;

    @JsonProperty("phoneNumber")
    @JsonAlias({
      "phone",
      "studentPhoneNumber",
      "Parents_Phone_Number",
      "Teacher_Phone_Number",
      "Director_Phone_Number"
    })
    private String phoneNumber;

    /** DB 비교용: 숫자만 남긴 전화번호 (하이픈/공백 제거) */
    public String normalizedPhone() {
        return phoneNumber == null ? "" : phoneNumber.replaceAll("\\D", "");
    }

    // ----- Getters / Setters -----

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
}
