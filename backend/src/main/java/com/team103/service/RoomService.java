package com.team103.service;

import com.team103.dto.RoomVectorLayoutRequest;
import com.team103.dto.RoomVectorLayoutResponse;
import com.team103.model.Room;
import com.team103.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Objects;

@Service
public class RoomService {
    private final RoomRepository roomRepo;

    public RoomService(RoomRepository roomRepo) {
        this.roomRepo = roomRepo;
    }

    public Room getOrCreate(int roomNumber, int academyNumber) {
        return roomRepo.findByRoomNumberAndAcademyNumber(roomNumber, academyNumber)
                .orElseGet(() -> {
                    Room r = new Room();
                    r.setRoomNumber(roomNumber);
                    r.setAcademyNumber(academyNumber);
                    // 기본 벡터값
                    r.setVectorVersion(1);
                    r.setVectorCanvasW(1000.0);
                    r.setVectorCanvasH(700.0);
                    r.setVectorLayout(new ArrayList<>());
                    return roomRepo.save(r);
                });
    }

    public RoomVectorLayoutResponse getVectorLayout(int roomNumber, int academyNumber) {
        Room r = getOrCreate(roomNumber, academyNumber);

        RoomVectorLayoutResponse res = new RoomVectorLayoutResponse();
        res.setVersion(Objects.requireNonNullElse(r.getVectorVersion(), 1));
        res.setCanvasW(Objects.requireNonNullElse(r.getVectorCanvasW(), 1000.0));
        res.setCanvasH(Objects.requireNonNullElse(r.getVectorCanvasH(), 700.0));
        res.setSeats(convertSeatsToResponseDto(r));  // ✅ studentId 포함
        return res;
    }

    public void putVectorLayout(RoomVectorLayoutRequest req) {
        validateReq(req);
        Room r = getOrCreate(req.getRoomNumber(), req.getAcademyNumber());
        overwriteFromDto(r, req);
        roomRepo.save(r);
    }

    public void patchVectorLayout(RoomVectorLayoutRequest req) {
        validateReq(req);
        Room r = roomRepo.findByRoomNumberAndAcademyNumber(req.getRoomNumber(), req.getAcademyNumber())
                .orElseThrow(() -> new IllegalArgumentException("room not found"));

        if (req.getVersion() != null) r.setVectorVersion(req.getVersion());
        if (req.getCanvasW() != null) r.setVectorCanvasW(req.getCanvasW());
        if (req.getCanvasH() != null) r.setVectorCanvasH(req.getCanvasH());
        if (req.getSeats() != null) r.setVectorLayout(convertSeatsToEntity(req)); // ✅ studentId 반영
        roomRepo.save(r);
    }

    public void clearVectorLayout(int roomNumber, int academyNumber) {
        Room r = roomRepo.findByRoomNumberAndAcademyNumber(roomNumber, academyNumber)
                .orElseThrow(() -> new IllegalArgumentException("room not found"));
        r.setVectorVersion(null);
        r.setVectorCanvasW(null);
        r.setVectorCanvasH(null);
        r.setVectorLayout(null);
        roomRepo.save(r);
    }

    private void validateReq(RoomVectorLayoutRequest req) {
        if (req.getAcademyNumber() == null) throw new IllegalArgumentException("academyNumber is required");
        if (req.getRoomNumber() == null) throw new IllegalArgumentException("roomNumber is required");
    }

    private void overwriteFromDto(Room r, RoomVectorLayoutRequest req) {
        r.setVectorVersion(Objects.requireNonNullElse(req.getVersion(), 1));
        r.setVectorCanvasW(Objects.requireNonNullElse(req.getCanvasW(), 1000.0));
        r.setVectorCanvasH(Objects.requireNonNullElse(req.getCanvasH(), 700.0));
        r.setVectorLayout(convertSeatsToEntity(req)); // ✅ studentId 반영
    }

    /** Request → Entity (✅ studentId/occupiedAt 매핑) */
    private java.util.List<Room.VectorSeat> convertSeatsToEntity(RoomVectorLayoutRequest req) {
        if (req.getSeats() == null) return null;
        var out = new ArrayList<Room.VectorSeat>(req.getSeats().size());
        for (var s : req.getSeats()) {
            Room.VectorSeat t = new Room.VectorSeat();
            t.setId(StringUtils.hasText(s.getId()) ? s.getId() : java.util.UUID.randomUUID().toString());
            t.setLabel(s.getLabel());
            t.setX(s.getX()); t.setY(s.getY()); t.setW(s.getW()); t.setH(s.getH());
            t.setR(s.getR()); t.setDisabled(s.getDisabled());
            // ✅ 추가: 학생 배정 저장
            t.setStudentId(s.getStudentId());
            // 배정이 있는 경우(선택) 저장 시간 찍기
           
            out.add(t);
        }
        return out;
    }

    /** Entity → Response (✅ studentId 매핑) */
    private java.util.List<RoomVectorLayoutResponse.VectorSeat> convertSeatsToResponseDto(Room r) {
        if (r.getVectorLayout() == null) return null;
        var out = new ArrayList<RoomVectorLayoutResponse.VectorSeat>(r.getVectorLayout().size());
        for (var s : r.getVectorLayout()) {
            RoomVectorLayoutResponse.VectorSeat t = new RoomVectorLayoutResponse.VectorSeat();
            t.setId(s.getId()); t.setLabel(s.getLabel());
            t.setX(s.getX()); t.setY(s.getY()); t.setW(s.getW()); t.setH(s.getH());
            t.setR(s.getR()); t.setDisabled(s.getDisabled());
            // ✅ 추가: 학생 배정 반환
            t.setStudentId(s.getStudentId());
            out.add(t);
        }
        return out;
    }
}
