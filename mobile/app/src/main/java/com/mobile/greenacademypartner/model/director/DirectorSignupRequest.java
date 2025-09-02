package com.mobile.greenacademypartner.model.director;

import java.util.List;

public class DirectorSignupRequest {
    private String name;               // Director_Name
    private String username;           // Director_ID
    private String password;           // Director_PW (평문 전송 → 서버에서 BCrypt)
    private String phone;              // Director_Phone_Number
    private List<Integer> academyNumbers; // Academy_Number

    public DirectorSignupRequest(String name, String username, String password, String phone, List<Integer> academyNumbers) {
        this.name = name;
        this.username = username;
        this.password = password;
        this.phone = phone;
        this.academyNumbers = academyNumbers;
    }

    public String getName() { return name; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getPhone() { return phone; }
    public List<Integer> getAcademyNumbers() { return academyNumbers; }

    public void setName(String name) { this.name = name; }
    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setAcademyNumbers(List<Integer> academyNumbers) { this.academyNumbers = academyNumbers; }
}
