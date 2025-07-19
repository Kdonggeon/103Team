package com.mobile.greenacademypartner.model;

public class Academy {
    private int id;
    private String academyName;

    public Academy() {
        // 기본 생성자 (필수)
    }

    // 🔧 이 생성자를 추가하세요
    public Academy(int id, String academyName) {
        this.id = id;
        this.academyName = academyName;
    }

    public int getId() {
        return id;
    }

    public String getAcademyName() {
        return academyName;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setAcademyName(String academyName) {
        this.academyName = academyName;
    }
}
