package com.team103.dto;

import java.util.List;

public class DirectorRoomView {
    private Integer roomNumber;
    private String className;       // 진행중/당일 배정 반 이름(없을 수 있음)
    private SeatBoardResponse seatBoard; // 해당 방의 좌석 현황 (카운트/웨이팅 포함)

    public Integer getRoomNumber() { return roomNumber; }
    public void setRoomNumber(Integer roomNumber) { this.roomNumber = roomNumber; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public SeatBoardResponse getSeatBoard() { return seatBoard; }
    public void setSeatBoard(SeatBoardResponse seatBoard) { this.seatBoard = seatBoard; }
}
