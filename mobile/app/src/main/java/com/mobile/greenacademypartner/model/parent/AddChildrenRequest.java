package com.mobile.greenacademypartner.model.parent;

import java.util.List;

public class AddChildrenRequest {
    private List<String> studentIds;

    public AddChildrenRequest(List<String> studentIds) {
        this.studentIds = studentIds;
    }

    public List<String> getStudentIds() {
        return studentIds;
    }

    public void setStudentIds(List<String> studentIds) {
        this.studentIds = studentIds;
    }
}
