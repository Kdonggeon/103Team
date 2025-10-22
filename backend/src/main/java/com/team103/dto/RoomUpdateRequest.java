package com.team103.dto;

import com.team103.model.Room;
import java.util.List;

public class RoomUpdateRequest {
    private Integer academyNumber;      // 필수 (어느 학원 방인지)
    private Integer rows;               // 선택
    private Integer cols;               // 선택
    private List<Room.SeatCell> layout; // 선택: 좌석 그리드 정보(1-base)
    private Room.CurrentClass currentClass; // 선택: 현재 수업 정보 교체
    private List<Room.Seat> seats;      // 선택: 좌석-학생 배치/출석 상태

    public Integer getAcademyNumber() { return academyNumber; }
    public void setAcademyNumber(Integer academyNumber) { this.academyNumber = academyNumber; }
    public Integer getRows() { return rows; }
    public void setRows(Integer rows) { this.rows = rows; }
    public Integer getCols() { return cols; }
    public void setCols(Integer cols) { this.cols = cols; }
    public List<Room.SeatCell> getLayout() { return layout; }
    public void setLayout(List<Room.SeatCell> layout) { this.layout = layout; }
    public Room.CurrentClass getCurrentClass() { return currentClass; }
    public void setCurrentClass(Room.CurrentClass currentClass) { this.currentClass = currentClass; }
    public List<Room.Seat> getSeats() { return seats; }
    public void setSeats(List<Room.Seat> seats) { this.seats = seats; }
}
