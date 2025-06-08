package com.mobile.greenacademypartner.model;

public class SignupRequest {
    private String id;
    private String pw;
    private String name;
    private String email;
    private String phone;

    public SignupRequest(String id, String pw, String name, String email, String phone) {
        this.id = id;
        this.pw = pw;
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    // getter/setter 생략 가능 (Gson이 알아서 처리)
}
