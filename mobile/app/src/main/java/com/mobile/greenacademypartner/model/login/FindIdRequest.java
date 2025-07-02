package com.mobile.greenacademypartner.model.login;

import com.google.gson.annotations.SerializedName;

public class FindIdRequest {

    private String name;

    @SerializedName("phone") // JSON 필드명이 "phone"일 때 직렬화 매핑
    private String phoneNumber;

    private String role;

    public FindIdRequest(String name, String phoneNumber, String role) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.role = role;
    }

    public String getName() { return name; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getPhone() { return phoneNumber; } // ✅ 추가됨
    public String getRole() { return role; }

    public void setName(String name) { this.name = name; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setRole(String role) { this.role = role; }
}
