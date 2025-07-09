package com.team103.dto;

import java.util.List;

public class AddChildrenRequest {
	//부모에서 자녀 추가 기능
    private List<String> studentIds; // 자녀(Student) ID 리스트

    public List<String> getStudentIds() {
        return studentIds;
    }

    public void setStudentIds(List<String> studentIds) {
        this.studentIds = studentIds;
    }
}
