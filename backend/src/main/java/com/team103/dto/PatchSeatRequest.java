package com.team103.dto;
/** PATCH 요청: 단건 배정/해제 */
public class PatchSeatRequest {
    public Integer roomNumber;           // 필수
    public String seatLabel;             // 필수
    public String studentId;             // null 이면 해제
}

