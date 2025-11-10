package com.team103.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;

@Document(collection = "seatmaps")
@CompoundIndex(name = "class_room_unique", def = "{'classId': 1, 'roomNumber': 1}", unique = true)
public class SeatMap {

    @Id
    private String id;

    private String classId;      // ex) "class1762297594607"
    private Integer roomNumber;  // ex) 403

    // 좌석라벨 -> 학생ID (null/빈 문자열이면 미배정으로 취급)
    private Map<String, String> map = new HashMap<>();

    public SeatMap() {}

    public SeatMap(String classId, Integer roomNumber) {
        this.classId = classId;
        this.roomNumber = roomNumber;
        this.map = new HashMap<>();
    }

    public String getId() { return id; }
    public String getClassId() { return classId; }
    public Integer getRoomNumber() { return roomNumber; }
    public Map<String, String> getMap() { return map; }

    public void setId(String id) { this.id = id; }
    public void setClassId(String classId) { this.classId = classId; }
    public void setRoomNumber(Integer roomNumber) { this.roomNumber = roomNumber; }
    public void setMap(Map<String, String> map) { this.map = map; }
}
