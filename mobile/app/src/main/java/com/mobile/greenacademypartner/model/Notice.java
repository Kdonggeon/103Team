package com.mobile.greenacademypartner.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Notice {

    @SerializedName("id")
    private String id;

    @SerializedName("title")
    private String title;

    @SerializedName("content")
    private String content;

    @SerializedName(value="createdAt", alternate={"created_at"})
    private String createdAt;

    @SerializedName("author")
    private String author;

    @SerializedName("teacherName")
    private String teacherName;

    @SerializedName("academyNumber")
    private Integer academyNumber;   // 👉 null 가능해야 함

    @SerializedName("academyNumbers")
    private List<Integer> academyNumbers;  // 👉 여러 학원 번호 지원

    @SerializedName(value="academyName", alternate={"academy_name"})
    private String academyName;

    // ---------------- getters & setters ----------------

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

    public Integer getAcademyNumber() { return academyNumber; }
    public void setAcademyNumber(Integer academyNumber) { this.academyNumber = academyNumber; }

    public List<Integer> getAcademyNumbers() { return academyNumbers; }
    public void setAcademyNumbers(List<Integer> academyNumbers) { this.academyNumbers = academyNumbers; }

    public String getAcademyName() { return academyName; }
    public void setAcademyName(String academyName) { this.academyName = academyName; }
}
