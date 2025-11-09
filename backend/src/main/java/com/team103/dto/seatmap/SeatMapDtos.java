package com.team103.dto.seatmap;

import java.util.Map;

public class SeatMapDtos {

    /** GET 응답 */
    public static class SeatMapResponse {
        public String classId;
        public Integer roomNumber;
        public Map<String, String> map; // label -> studentId
    }

    /** PATCH 단건 배정/해제 요청 */
    public static class AssignSeatRequest {
        public Integer roomNumber;   // 필수
        public String seatLabel;     // 필수 (좌석 라벨)
        public String studentId;     // null 또는 빈 문자열이면 배정 해제
    }

    /** PUT 벌크 저장 요청 (선택) */
    public static class PutSeatMapRequest {
        public Integer roomNumber;   // 필수
        public Map<String, String> map; // 전체 맵 교체
    }
}
