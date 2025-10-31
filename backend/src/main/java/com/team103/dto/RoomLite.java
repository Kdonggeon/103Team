package com.team103.dto;

public class RoomLite {
    private int academyNumber;
    private int roomNumber;
    private boolean hasVector;
    private int vectorSeatCount;

    public int getAcademyNumber() { return academyNumber; }
    public void setAcademyNumber(int academyNumber) { this.academyNumber = academyNumber; }

    public int getRoomNumber() { return roomNumber; }
    public void setRoomNumber(int roomNumber) { this.roomNumber = roomNumber; }

    public boolean isHasVector() { return hasVector; }
    public void setHasVector(boolean hasVector) { this.hasVector = hasVector; }

    public int getVectorSeatCount() { return vectorSeatCount; }
    public void setVectorSeatCount(int vectorSeatCount) { this.vectorSeatCount = vectorSeatCount; }
}
