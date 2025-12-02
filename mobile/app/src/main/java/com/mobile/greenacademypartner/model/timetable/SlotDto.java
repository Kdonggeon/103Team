package com.mobile.greenacademypartner.model.timetable;

import com.google.gson.annotations.SerializedName;

public class SlotDto {

    @SerializedName("classId")
    public String classId;

    @SerializedName("className")
    public String className;

    @SerializedName("date")  // "YYYY-MM-DD"
    public String date;

    @SerializedName("dayOfWeek")
    public int dayOfWeek;   // 1~7 (월~일)

    @SerializedName("startTime")
    public String startTime; // "HH:mm"

    @SerializedName("endTime")
    public String endTime;

    @SerializedName("roomNumber")
    public Integer roomNumber;

    @SerializedName("academyNumber")
    public Integer academyNumber;

    @SerializedName("academyName")
    public String academyName;
}
