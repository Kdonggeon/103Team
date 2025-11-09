package com.team103.dto;

import java.util.List;

public class DirectorOverviewResponse {

    private String date;
    private List<RoomStatus> rooms;
    private List<SeatBoardResponse.WaitingItem> waiting; // 공통 DTO 재활용

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public List<RoomStatus> getRooms() { return rooms; }
    public void setRooms(List<RoomStatus> rooms) { this.rooms = rooms; }

    public List<SeatBoardResponse.WaitingItem> getWaiting() { return waiting; }
    public void setWaiting(List<SeatBoardResponse.WaitingItem> waiting) { this.waiting = waiting; }

    public static class RoomStatus {
        private int roomNumber;
        private String className;
        private List<SeatBoardResponse.SeatStatus> seats;
        private Integer presentCount;
        private Integer lateCount;
        private Integer absentCount;
        private Integer moveOrBreakCount;
        private Integer notRecordedCount;

        public int getRoomNumber() { return roomNumber; }
        public void setRoomNumber(int roomNumber) { this.roomNumber = roomNumber; }

        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }

        public List<SeatBoardResponse.SeatStatus> getSeats() { return seats; }
        public void setSeats(List<SeatBoardResponse.SeatStatus> seats) { this.seats = seats; }

        public Integer getPresentCount() { return presentCount; }
        public void setPresentCount(Integer presentCount) { this.presentCount = presentCount; }
        public Integer getLateCount() { return lateCount; }
        public void setLateCount(Integer lateCount) { this.lateCount = lateCount; }
        public Integer getAbsentCount() { return absentCount; }
        public void setAbsentCount(Integer absentCount) { this.absentCount = absentCount; }
        public Integer getMoveOrBreakCount() { return moveOrBreakCount; }
        public void setMoveOrBreakCount(Integer moveOrBreakCount) { this.moveOrBreakCount = moveOrBreakCount; }
        public Integer getNotRecordedCount() { return notRecordedCount; }
        public void setNotRecordedCount(Integer notRecordedCount) { this.notRecordedCount = notRecordedCount; }
    }
}
