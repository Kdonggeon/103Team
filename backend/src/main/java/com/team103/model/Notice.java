package com.team103.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Document(collection = "notices")
public class Notice {

    @Id
    private String id;

    private String title;
    private String content;

    /** 작성자(교사 ID 그대로 저장) */
    private String author;

    /** 표시용 교사명 */
    private String teacherName;

    /** 생성 시각 */
    private Date createdAt;

    /** 단일 학원 번호(하위호환용) */
    private Integer academyNumber;

    /** ✅ 여러 학원 번호 지원 */
    private List<Integer> academyNumbers;

    /** 업로드된 이미지의 공개 URL 목록 */
    private List<String> imageUrls;

    /** ✅ 과목(반) 연결(선택) */
    private String classId;     // 예: "class101"
    private String className;   // 예: "수학"

    /** 기본 생성자에서 createdAt 초기화 */
    public Notice() {
        this.createdAt = new Date();
    }

    // --- getters & setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Integer getAcademyNumber() { return academyNumber; }
    public void setAcademyNumber(Integer academyNumber) { this.academyNumber = academyNumber; }

    public List<Integer> getAcademyNumbers() { return academyNumbers; }
    public void setAcademyNumbers(List<Integer> academyNumbers) { this.academyNumbers = academyNumbers; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
}
