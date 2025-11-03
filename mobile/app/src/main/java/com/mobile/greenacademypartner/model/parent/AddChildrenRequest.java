package com.mobile.greenacademypartner.model.parent;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class AddChildrenRequest {

    @SerializedName("studentIds")   // ✅ 서버와 JSON 키 일치
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
