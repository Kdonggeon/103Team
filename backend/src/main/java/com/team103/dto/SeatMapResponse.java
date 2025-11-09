package com.team103.dto;

import java.util.Map;

/** GET 응답 */
public class SeatMapResponse {
    public String classId;
    public Integer roomNumber;
    public Map<String,String> map;
    public SeatMapResponse() {}
    public SeatMapResponse(String classId, Integer roomNumber, Map<String,String> map){
        this.classId = classId;
        this.roomNumber = roomNumber;
        this.map = map;
    }
}

