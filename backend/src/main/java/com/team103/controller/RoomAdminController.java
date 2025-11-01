package com.team103.controller;

import com.team103.dto.RoomLite;
import com.team103.dto.RoomVectorLayoutRequest;
import com.team103.dto.RoomVectorLayoutResponse;
import com.team103.model.Room;
import com.team103.repository.RoomRepository;
import com.team103.service.RoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/rooms")
public class RoomAdminController {

    private final RoomRepository roomRepo;
    private final RoomService roomService;

    public RoomAdminController(RoomRepository roomRepo, RoomService roomService) {
        this.roomRepo = roomRepo;
        this.roomService = roomService;
    }

    /** ✅ 학원 내 강의실 목록(간단 DTO) */
    @GetMapping
    public List<RoomLite> list(@RequestParam Integer academyNumber) {
        var rooms = roomRepo.findByAcademyNumber(academyNumber);
        var out = new ArrayList<RoomLite>(rooms.size());
        for (Room r : rooms) {
            var d = new RoomLite();
            d.setAcademyNumber(r.getAcademyNumber());
            d.setRoomNumber(r.getRoomNumber());
            d.setHasVector(r.getVectorVersion() != null && r.getVectorLayout() != null);
            d.setVectorSeatCount(r.getVectorLayout() == null ? 0 : r.getVectorLayout().size());
            out.add(d);
        }
        return out;
    }

    /** 상세 (없으면 생성) */
    @GetMapping("/{roomNumber}")
    public Room detail(@PathVariable Integer roomNumber,
                       @RequestParam Integer academyNumber) {
        return roomService.getOrCreate(roomNumber, academyNumber);
    }

    /** 벡터 레이아웃 조회 */
    @GetMapping("/{roomNumber}/vector-layout")
    public RoomVectorLayoutResponse getVector(@PathVariable Integer roomNumber,
                                              @RequestParam Integer academyNumber) {
        return roomService.getVectorLayout(roomNumber, academyNumber);
    }

    /** 저장(전체 교체) – query/path 값을 DTO에 주입 */
    @PutMapping("/{roomNumber}/vector-layout")
    public ResponseEntity<?> putVector(@PathVariable Integer roomNumber,
                                       @RequestParam Integer academyNumber,
                                       @RequestBody RoomVectorLayoutRequest req) {
        if (req.getAcademyNumber() == null) req.setAcademyNumber(academyNumber);
        if (req.getRoomNumber() == null) req.setRoomNumber(roomNumber);
        roomService.putVectorLayout(req);
        return ResponseEntity.ok(Map.of("message", "saved"));
    }

    /** 부분 수정(PATCH) – query/path 값을 DTO에 주입 */
    @PatchMapping("/{roomNumber}/vector-layout")
    public ResponseEntity<?> patchVector(@PathVariable Integer roomNumber,
                                         @RequestParam Integer academyNumber,
                                         @RequestBody RoomVectorLayoutRequest req) {
        if (req.getAcademyNumber() == null) req.setAcademyNumber(academyNumber);
        if (req.getRoomNumber() == null) req.setRoomNumber(roomNumber);
        roomService.patchVectorLayout(req);
        return ResponseEntity.ok(Map.of("message", "patched"));
    }

    /** 벡터 레이아웃 초기화(삭제) */
    @DeleteMapping("/{roomNumber}/vector-layout")
    public ResponseEntity<?> clearVector(@PathVariable Integer roomNumber,
                                         @RequestParam Integer academyNumber) {
        roomService.clearVectorLayout(roomNumber, academyNumber);
        return ResponseEntity.noContent().build();
    }

    /** 방 삭제(문서 자체 삭제) */
    @DeleteMapping("/{roomNumber}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Integer roomNumber,
                                           @RequestParam Integer academyNumber) {
        var opt = roomRepo.findByRoomNumberAndAcademyNumber(roomNumber, academyNumber);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        roomRepo.delete(opt.get());
        return ResponseEntity.noContent().build();
    }

    /** (선택) 서비스에서 던진 IllegalArgumentException을 400으로 변환 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }
}
