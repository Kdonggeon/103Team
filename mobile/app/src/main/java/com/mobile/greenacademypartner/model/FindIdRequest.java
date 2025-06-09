package com.mobile.greenacademypartner.model;

public class FindIdRequest {

    private String name;
    private long phoneNumber;
    private String role;

    public FindIdRequest(String name, long phoneNumber, String role) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.role = role;
    }

    public String getName() { return name; }
    public long getPhoneNumber() { return phoneNumber; }
    public String getRole() { return role; }

    public void setName(String name) { this.name = name; }
    public void setPhoneNumber(long phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setRole(String role) { this.role = role; }
}
