package com.mobile.greenacademypartner.util;

import com.google.gson.*;
import com.mobile.greenacademypartner.model.LoginResponse;

import java.lang.reflect.Type;

public class LoginResponseDeserializer implements JsonDeserializer<LoginResponse> {
    @Override
    public LoginResponse deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        LoginResponse res = new LoginResponse();

        res.setStatus(obj.get("status").getAsString());
        res.setRole(obj.get("role").getAsString());
        res.setUsername(obj.get("username").getAsString());
        res.setName(obj.get("name").getAsString());
        res.setToken(obj.get("token").getAsString());

        // 역할별 전화번호 필드 추출
        if (obj.has("Student_Phone_Number")) {
            res.setPhone(obj.get("Student_Phone_Number").getAsString());
        } else if (obj.has("Parents_Phone_Number")) {
            res.setPhone(obj.get("Parents_Phone_Number").getAsString());
        } else if (obj.has("Teacher_Phone_Number")) {
            res.setPhone(obj.get("Teacher_Phone_Number").getAsString());
        }

        // 학생일 경우에만 추가 필드 있음
        if (obj.has("Student_Address")) {
            res.setAddress(obj.get("Student_Address").getAsString());
        }
        if (obj.has("School")) {
            res.setSchool(obj.get("School").getAsString());
        }
        if (obj.has("Grade")) {
            res.setGrade(obj.get("Grade").getAsInt());
        }
        if (obj.has("Gender")) {
            res.setGender(obj.get("Gender").getAsString());
        }

        return res;
    }
}
