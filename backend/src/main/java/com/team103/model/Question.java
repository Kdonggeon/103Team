package com.team103.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;
import java.util.List;

@Document(collection = "questions")
@JsonInclude(JsonInclude.Include.NON_NULL) // null 필드는 생략
public class Question {

    @Id
    @JsonProperty("id")
    private String id;

    // 프런트 호환용: _id도 같이 노출
    @JsonProperty("_id")
    public String get_id() { return id; }

    private String title;
    private String content;
    private String author;        // 학생/학부모 아이디
    private Date createdAt;
    private String authorRole;
    private int academyNumber;

    private List<String> teacherNames; // 답변한 교사 이름들(중복 제거, 순서 유지)
    private String academyName;

    @Field("room")
    private Boolean room;

    private String roomParentId;      // 학부모 방 키
    private String roomStudentId;     // 학생 방 키

    // ------- 프론트 표시 보강용(저장 안함) -------
    @Transient
    private List<String> recentResponderNames;
    

    @Transient
    private int unreadCount;

    // ✅ 마지막 교사 답변 시각(TeacherQnaPanel의 latestActivityTs가 사용)
    @Transient
    private Date lastAnswerAt;

    // ✅ 질문/답변/팔로업 중 가장 최신(정렬/미확인 판단 기준)
    @Transient
    private Date updatedAt;

    public Question() {
        this.createdAt = new Date();
    }

    // ---------- getters & setters ----------
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public String getAuthorRole() { return authorRole; }
    public void setAuthorRole(String authorRole) { this.authorRole = authorRole; }

    public int getAcademyNumber() { return academyNumber; }
    public void setAcademyNumber(int academyNumber) { this.academyNumber = academyNumber; }

    public List<String> getTeacherNames() { return teacherNames; }
    public void setTeacherNames(List<String> teacherNames) { this.teacherNames = teacherNames; }

    public String getAcademyName() { return academyName; }
    public void setAcademyName(String academyName) { this.academyName = academyName; }

    public Boolean getRoom() { return room; }
    public void setRoom(Boolean room) { this.room = room; }

    public List<String> getRecentResponderNames(){ return recentResponderNames; }
    public void setRecentResponderNames(List<String> v){ this.recentResponderNames = v; }

    public int getUnreadCount(){ return unreadCount; }
    public void setUnreadCount(int v){ this.unreadCount = v; }

    public String getRoomStudentId() { return roomStudentId; }
    public void setRoomStudentId(String roomStudentId) { this.roomStudentId = roomStudentId; }

    public String getRoomParentId() { return roomParentId; }
    public void setRoomParentId(String roomParentId) { this.roomParentId = roomParentId; }

    public Date getLastAnswerAt(){ return lastAnswerAt; }
    public void setLastAnswerAt(Date v){ this.lastAnswerAt = v; }
    
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
