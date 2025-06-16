package com.mobile.greenacademypartner.model;

public class CreateClassRequest {
    private String className;
    private String teacherId;
    private String schedule;

    public CreateClassRequest(String className, String teacherId, String schedule) {
        this.className = className;
        this.teacherId = teacherId;
        this.schedule = schedule;
    }

    // Getter 생략 가능 (Retrofit 직렬화용 생성자만 있으면 됨)
}
