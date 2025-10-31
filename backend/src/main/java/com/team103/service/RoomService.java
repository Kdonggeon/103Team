package com.team103.service;

import com.team103.dto.RoomVectorLayoutRequest;
import com.team103.dto.RoomVectorLayoutResponse;
import com.team103.model.Room;
import com.team103.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
                    return roomRepo.save(r);
                });
    }

    public RoomVectorLayoutResponse getVectorLayout(int roomNumber, int academyNumber) {
        Room r = roomRepo.findByRoomNumberAndAcademyNumber(roomNumber, academyNumber)
                .orElseThrow(() -> new IllegalArgumentException("room not found"));

        // 저장된 벡터가 없으면 빈 응답 반환(프론트에서 기본 배치 생성)
        if (r.getVectorVersion() == null) return new RoomVectorLayoutResponse();

        RoomVectorLayoutResponse res = new RoomVectorLayoutResponse();
        res.setVersion(r.getVectorVersion());
        res.setCanvasW(r.getVectorCanvasW());
        res.setCanvasH(r.getVectorCanvasH());
        res.setSeats(convertSeatsToResponseDto(r));  // ✅ Response 타입으로 변환
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
        // null 아닌 필드만 반영
        if (req.getVersion() != null) r.setVectorVersion(req.getVersion());
        if (req.getCanvasW() != null) r.setVectorCanvasW(req.getCanvasW());
        if (req.getCanvasH() != null) r.setVectorCanvasH(req.getCanvasH());
        if (req.getSeats() != null) r.setVectorLayout(convertSeatsToEntity(req));
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
        r.setVectorCanvasW(Objects.requireNonNullElse(req.getCanvasW(), 1.0));
        r.setVectorCanvasH(Objects.requireNonNullElse(req.getCanvasH(), 1.0));
        r.setVectorLayout(convertSeatsToEntity(req));
    }

    /** Request → Entity */
    private java.util.List<Room.VectorSeat> convertSeatsToEntity(RoomVectorLayoutRequest req) {
        if (req.getSeats() == null) return null;
        var out = new ArrayList<Room.VectorSeat>(req.getSeats().size());
        for (var s : req.getSeats()) {
            Room.VectorSeat t = new Room.VectorSeat();
            t.setId(StringUtils.hasText(s.getId()) ? s.getId() : java.util.UUID.randomUUID().toString());
            t.setLabel(s.getLabel());
            t.setX(s.getX()); t.setY(s.getY()); t.setW(s.getW()); t.setH(s.getH());
            t.setR(s.getR()); t.setDisabled(s.getDisabled());
            out.add(t);
        }
        return out;
    }

    /** Entity → Response */
    private java.util.List<RoomVectorLayoutResponse.VectorSeat> convertSeatsToResponseDto(Room r) {
        if (r.getVectorLayout() == null) return null;
        var out = new ArrayList<RoomVectorLayoutResponse.VectorSeat>(r.getVectorLayout().size());
        for (var s : r.getVectorLayout()) {
            RoomVectorLayoutResponse.VectorSeat t = new RoomVectorLayoutResponse.VectorSeat();
            t.setId(s.getId()); t.setLabel(s.getLabel());
            t.setX(s.getX()); t.setY(s.getY()); t.setW(s.getW()); t.setH(s.getH());
            t.setR(s.getR()); t.setDisabled(s.getDisabled());
            out.add(t);
        }
        return out;
    }
}
