package com.mobile.greenacademypartner.model.login;

import com.google.gson.annotations.SerializedName;

public class FindIdRequest {

    @SerializedName("name")
    private String name;

    // ✅ 백엔드 DTO와 동일한 필드명 사용
    @SerializedName("phoneNumber")
    private String phoneNumber;

    @SerializedName("role")
    private String role;

    // ✅ 기본 생성자 (Gson용)
    public FindIdRequest() {}

    // ✅ 전체 생성자
    public FindIdRequest(String name, String phoneNumber, String role) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.role = role;
    }

    // ✅ Getter
    public String getName() { return name; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getRole() { return role; }

    // ✅ Setter
    public void setName(String name) { this.name = name; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setRole(String role) { this.role = role; }
}