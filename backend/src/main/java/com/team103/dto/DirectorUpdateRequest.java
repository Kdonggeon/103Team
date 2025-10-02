package com.team103.dto;

import java.util.List;

public class DirectorUpdateRequest {

    // 로그인 ID (Director 모델의 username 필드와 매핑됨)
    private String directorId;

    // 표시 이름
    private String directorName;

    // 연락처 (Director 모델의 phone 필드와 매핑)
    private String directorPhoneNumber;

    // 여러 학원 번호 지원 (Director 모델의 academyNumbers와 매핑)
    private List<Integer> academyNumbers;

    public DirectorUpdateRequest() {}

    public String getDirectorId() { return directorId; }
    public void setDirectorId(String directorId) { this.directorId = directorId; }

    public String getDirectorName() { return directorName; }
    public void setDirectorName(String directorName) { this.directorName = directorName; }

    public String getDirectorPhoneNumber() { return directorPhoneNumber; }
    public void setDirectorPhoneNumber(String directorPhoneNumber) { this.directorPhoneNumber = directorPhoneNumber; }

    public List<Integer> getAcademyNumbers() { return academyNumbers; }
    public void setAcademyNumbers(List<Integer> academyNumbers) { this.academyNumbers = academyNumbers; }
}
