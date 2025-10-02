package com.team103.dto;

public class AccountDeleteRequest {
    private String role;      // student|teacher|parent|director
    private String id;        // 로그인 아이디( studentId / teacherId / parentsId / director username )
    private String password;  // 현재 비밀번호(평문)
    private String reason;    // (선택)
    private boolean confirm;  // "탈퇴확인" 체크박스

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public boolean isConfirm() { return confirm; }
    public void setConfirm(boolean confirm) { this.confirm = confirm; }
}
