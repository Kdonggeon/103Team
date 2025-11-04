package com.mobile.greenacademypartner.model;

import com.google.gson.annotations.SerializedName;

public class Academy {

    @SerializedName("id")
    private String id;   // 🚨 String으로 변경

    @SerializedName("academyNumber")
    private int academyNumber;

    @SerializedName(value = "academyName", alternate = {"name"})
    private String academyName;   // 🚨 서버의 "name" 키와 매핑

    @SerializedName("phone")
    private String phone;

    @SerializedName("address")
    private String address;

    @SerializedName("directorNumber")
    private int directorNumber;

    public Academy() {}

    public Academy(String id, int academyNumber, String academyName,
                   String phone, String address, int directorNumber) {
        this.id = id;
        this.academyNumber = academyNumber;
        this.academyName = academyName;
        this.phone = phone;
        this.address = address;
        this.directorNumber = directorNumber;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getAcademyNumber() { return academyNumber; }
    public void setAcademyNumber(int academyNumber) { this.academyNumber = academyNumber; }

    public String getAcademyName() { return academyName; }
    public void setAcademyName(String academyName) { this.academyName = academyName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public int getDirectorNumber() { return directorNumber; }
    public void setDirectorNumber(int directorNumber) { this.directorNumber = directorNumber; }
}
