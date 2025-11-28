package com.team103.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AccountDeleteRequest {
    @NotBlank(message = "role은 필수입니다.")
    private String role;      // student|teacher|parent|director

    @NotBlank(message = "id는 필수입니다.")
    private String id;        // 로그인 아이디( studentId / teacherId / parentsId / director username )

    @NotBlank(message = "password는 필수입니다.")
    @Size(min = 4, message = "password는 4자 이상이어야 합니다.")
    private String password;  // 현재 비밀번호(평문)

    private String reason;    // (선택)

    @AssertTrue(message = "탈퇴 확인에 동의해야 합니다.")
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
