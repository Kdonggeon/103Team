package com.team103.model;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "classes")
public class Course {


	@Id
    private String id;

    @Field("Class_ID")
    private String classId;

    @Field("Class_Name")
    private String className;

    @Field("Teacher_ID")
    private String teacherId;

    @Field("Students")
    private List<String> students;

    @Field("Schedule")
    private String schedule;

    public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getClassId() {
		return classId;
	}

	public void setClassId(String classId) {
		this.classId = classId;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getTeacherId() {
		return teacherId;
	}

	public void setTeacherId(String teacherId) {
		this.teacherId = teacherId;
	}

	public List<String> getStudents() {
	    return students;
	}

	public void setStudents(List<String> students) {
	    this.students = students;
	}

	public String getSchedule() {
		return schedule;
	}

	public void setSchedule(String schedule) {
		this.schedule = schedule;
	}
	
	
}
