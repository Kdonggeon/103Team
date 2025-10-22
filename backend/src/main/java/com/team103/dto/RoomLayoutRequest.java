package com.team103.dto;

import com.team103.model.Room;
import java.util.List;

public class RoomLayoutRequest {
    private Integer academyNumber;
    private Integer rows;
    private Integer cols;
    private List<Room.SeatCell> layout;

    public Integer getAcademyNumber() { return academyNumber; }
    public void setAcademyNumber(Integer academyNumber) { this.academyNumber = academyNumber; }
    public Integer getRows() { return rows; }
    public void setRows(Integer rows) { this.rows = rows; }
    public Integer getCols() { return cols; }
    public void setCols(Integer cols) { this.cols = cols; }
    public List<Room.SeatCell> getLayout() { return layout; }
    public void setLayout(List<Room.SeatCell> layout) { this.layout = layout; }
}
