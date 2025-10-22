package com.team103.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.team103.model.Room;
import com.team103.repository.RoomRepository;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    @Autowired
    private RoomRepository roomRepository;

    /** 수업 시작 시 현재 수업 정보 등록 */
    @PutMapping("/{roomNumber}/start-class")
    public ResponseEntity<?> startClass(
            @PathVariable int roomNumber,
            @RequestParam int academyNumber,
            @RequestBody Room.CurrentClass currentClass) {

        Room room = roomRepository.findByRoomNumberAndAcademyNumber(roomNumber, academyNumber)
            .orElse(null);
        if (room == null)
            return ResponseEntity.status(404).body("해당 강의실을 찾을 수 없습니다");

        room.setCurrentClass(currentClass);
        roomRepository.save(room);
        return ResponseEntity.ok("수업이 등록되었습니다");
    }

    /** QR 스캔 시 학생 출석 체크 */
    @PutMapping("/{roomNumber}/check-in")
    public ResponseEntity<?> checkIn(
            @PathVariable int roomNumber,
            @RequestParam int academyNumber,
            @RequestParam int seatNumber,
            @RequestParam String studentId) {

        Room room = roomRepository.findByRoomNumberAndAcademyNumber(roomNumber, academyNumber)
            .orElse(null);
        if (room == null) return ResponseEntity.status(404).body("강의실 없음");

        List<Room.Seat> seats = room.getSeats();
        if (seats == null) return ResponseEntity.status(400).body("좌석 데이터 없음");

        boolean updated = false;
        for (Room.Seat seat : seats) {
            if (seat.getSeatNumber() == seatNumber && studentId.equals(seat.getStudentId())) {
                seat.setCheckedIn(true);
                updated = true;
                break;
            }
        }

        if (!updated) return ResponseEntity.status(400).body("좌석 정보 불일치");

        roomRepository.save(room);
        return ResponseEntity.ok("출석 체크 완료");
    }
}
