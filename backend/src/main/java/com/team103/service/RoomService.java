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


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    
    public void putVectorLayoutFlexible(int roomNumber, int academyNumber, JsonNode body) {
        Room r = getOrCreate(roomNumber, academyNumber);

        // 메타(버전/캔버스)
        Integer ver = pickInt(body, "vectorVersion", "version");
        Double cw   = pickD(body, "vectorCanvasW", "canvasW");
        Double ch   = pickD(body, "vectorCanvasH", "canvasH");
        r.setVectorVersion(ver != null ? ver : 1);
        r.setVectorCanvasW(cw   != null ? cw   : 1000.0);
        r.setVectorCanvasH(ch   != null ? ch   : 700.0);

        // 좌석 배열: V2 우선, 없으면 seats
        JsonNode seats = body.get("vectorLayoutV2");
        if (seats == null || !seats.isArray()) seats = body.get("seats");

        var list = new ArrayList<Room.VectorSeat>();
        if (seats != null && seats.isArray()) {
            for (JsonNode n : seats) {
                Room.VectorSeat s = new Room.VectorSeat();
                s.setId(text(n, "_id", "id", "seatId", "seat_id", "label"));
                if (!StringUtils.hasText(s.getId())) s.setId(java.util.UUID.randomUUID().toString());
                s.setLabel(text(n, "label", "seatNumber", "name"));
                s.setX(d(n, "x")); s.setY(d(n, "y"));
                s.setW(d(n, "w")); s.setH(d(n, "h"));
                s.setR(d(n, "r"));
                s.setDisabled(bool(n, "disabled", "isDisabled"));
                // V2: Student_ID, 레거시: studentId 등
                s.setStudentId(text(n, "Student_ID", "studentId", "student_id", "student"));
                list.add(s);
            }
        }
        r.setVectorLayout(list);
        roomRepo.save(r);
    }

    /** (선택) PATCH 버전 */
    public void patchVectorLayoutFlexible(int roomNumber, int academyNumber, JsonNode body) {
        Room r = roomRepo.findByRoomNumberAndAcademyNumber(roomNumber, academyNumber)
                .orElseThrow(() -> new IllegalArgumentException("room not found"));

        Integer ver = pickInt(body, "vectorVersion", "version");
        Double cw   = pickD(body, "vectorCanvasW", "canvasW");
        Double ch   = pickD(body, "vectorCanvasH", "canvasH");
        if (ver != null) r.setVectorVersion(ver);
        if (cw  != null) r.setVectorCanvasW(cw);
        if (ch  != null) r.setVectorCanvasH(ch);

        JsonNode seats = body.get("vectorLayoutV2");
        if (seats == null || !seats.isArray()) seats = body.get("seats");
        if (seats != null && seats.isArray()) {
            var list = new ArrayList<Room.VectorSeat>();
            for (JsonNode n : seats) {
                Room.VectorSeat s = new Room.VectorSeat();
                s.setId(text(n, "_id", "id", "seatId", "seat_id", "label"));
                if (!StringUtils.hasText(s.getId())) s.setId(java.util.UUID.randomUUID().toString());
                s.setLabel(text(n, "label", "seatNumber", "name"));
                s.setX(d(n, "x")); s.setY(d(n, "y"));
                s.setW(d(n, "w")); s.setH(d(n, "h"));
                s.setR(d(n, "r"));
                s.setDisabled(bool(n, "disabled", "isDisabled"));
                s.setStudentId(text(n, "Student_ID", "studentId", "student_id", "student"));
                list.add(s);
            }
            r.setVectorLayout(list);
        }
        roomRepo.save(r);
    }

    /* ── helpers ───────────────── */
    private static Integer pickInt(JsonNode n, String a, String b) {
        JsonNode x = n.get(a); if (x==null) x = n.get(b);
        return (x!=null && x.isNumber()) ? x.intValue() : null;
    }
    private static Double pickD(JsonNode n, String a, String b) {
        JsonNode x = n.get(a); if (x==null) x = n.get(b);
        return (x!=null && x.isNumber()) ? x.doubleValue() : null;
    }
    private static String text(JsonNode n, String... keys) {
        for (String k : keys) {
            JsonNode v = n.get(k);
            if (v != null && !v.isNull()) return v.asText();
        }
        return null;
    }
    private static Double d(JsonNode n, String k) {
        JsonNode v = n.get(k); return (v!=null && v.isNumber()) ? v.doubleValue() : null;
    }
    private static Boolean bool(JsonNode n, String... keys) {
        for (String k: keys) {
            JsonNode v = n.get(k);
            if (v!=null && v.isBoolean()) return v.booleanValue();
        }
        return null;
    }
}
