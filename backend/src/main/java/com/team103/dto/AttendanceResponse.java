package com.team103.dto;

public class AttendanceResponse {
    public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	private String className;
    private String date;
    private String status;

    // Constructor
    public AttendanceResponse(String className, String date, String status) {
        this.className = className;
        this.date = date;
        this.status = status;
    }

    // Getter
}

