package com.team103.controller;

import com.team103.dto.RoomLayoutRequest;
import com.team103.dto.RoomUpdateRequest;
import com.team103.model.Room;
import com.team103.repository.RoomRepository;
import com.team103.service.RoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/rooms")
public class RoomAdminController {

    private final RoomRepository roomRepo;
    private final RoomService roomService;

    public RoomAdminController(RoomRepository roomRepo, RoomService roomService) {
        this.roomRepo = roomRepo;
        this.roomService = roomService;
    }

    @GetMapping
    public List<Room> list(@RequestParam Integer academyNumber) {
        return roomRepo.findByAcademyNumber(academyNumber);
    }

    @GetMapping("/{roomNumber}")
    public Room detail(@PathVariable Integer roomNumber,
                       @RequestParam Integer academyNumber) {
        return roomService.getOrCreate(roomNumber, academyNumber);
    }

    @PutMapping("/{roomNumber}/layout")
    public Room saveLayout(@PathVariable Integer roomNumber,
                           @RequestBody RoomLayoutRequest req) {
        return roomService.saveLayout(roomNumber, req);
    }

    @DeleteMapping("/{roomNumber}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Integer roomNumber,
                                           @RequestParam Integer academyNumber) {
        var opt = roomRepo.findByRoomNumberAndAcademyNumber(roomNumber, academyNumber);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        roomRepo.delete(opt.get());
        return ResponseEntity.noContent().build(); 
    }


    /** ✅ 부분 수정: rows/cols/layout/currentClass/seats 중 전달된 필드만 반영 */
    @PatchMapping("/{roomNumber}")
    public Room patchRoom(@PathVariable Integer roomNumber,
                          @RequestBody RoomUpdateRequest req) {
        return roomService.patchRoom(roomNumber, req);
    }
}
