package com.team103.controller;

import com.team103.dto.RoomLayoutRequest;
import com.team103.model.Room;
import com.team103.repository.RoomRepository;
import com.team103.service.RoomService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/rooms")
public class RoomAdminController {

    private final RoomRepository roomRepo;
    private final RoomService roomService;

    // 명시적 생성자 주입
    public RoomAdminController(RoomRepository roomRepo, RoomService roomService) {
        this.roomRepo = roomRepo;
        this.roomService = roomService;
    }

    // 학원 번호로 방 목록 조회
    @GetMapping
    public List<Room> list(@RequestParam Integer academyNumber) {
        return roomRepo.findByAcademyNumber(academyNumber);
    }

    // 방 상세(없으면 생성)
    @GetMapping("/{roomNumber}")
    public Room detail(@PathVariable Integer roomNumber,
                       @RequestParam Integer academyNumber) {
        return roomService.getOrCreate(roomNumber, academyNumber);
    }

    // 레이아웃 저장
    @PutMapping("/{roomNumber}/layout")
    public Room saveLayout(@PathVariable Integer roomNumber,
                           @RequestBody RoomLayoutRequest req) {
        return roomService.saveLayout(roomNumber, req);
    }
}
