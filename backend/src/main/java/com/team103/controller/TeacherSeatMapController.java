package com.team103.controller;

import com.team103.dto.seatmap.SeatMapDtos.AssignSeatRequest;
import com.team103.dto.seatmap.SeatMapDtos.PutSeatMapRequest;
import com.team103.dto.seatmap.SeatMapDtos.SeatMapResponse;
import com.team103.model.Course;
import com.team103.repository.CourseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 좌석 배정은 Course.seatMap (roomNumber -> (label -> studentId))에 저장한다.
 * 프론트 사용 경로:
 *  - GET   /api/manage/teachers/classes/{classId}/seatmap?roomNumber=403
 *  - PATCH /api/manage/teachers/classes/{classId}/seatmap        (단건 배정/해제)
 *  - PUT   /api/manage/teachers/classes/{classId}/seatmap        (벌크 치환)
 */
@RestController
@RequestMapping("/api/manage/teachers/classes")
@CrossOrigin(origins = "*")
public class TeacherSeatMapController {

    private final CourseRepository courseRepo;

    public TeacherSeatMapController(CourseRepository courseRepo) {
        this.courseRepo = courseRepo;
    }

    @GetMapping("/{classId}/seatmap")
    public ResponseEntity<?> getSeatMap(
            @PathVariable String classId,
            @RequestParam(name = "roomNumber") Integer roomNumber
    ) {
        if (roomNumber == null) {
            return ResponseEntity.badRequest().body(Map.of("message","roomNumber is required"));
        }

        // ✅ 네 레포 시그니처에 맞게 Optional 처리
        Course c = courseRepo.findByClassId(classId).orElse(null);
        if (c == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message","class not found"));
        }

        Map<Integer, Map<String, String>> seatMap = c.getSeatMap();
        Map<String,String> map = (seatMap != null) ? seatMap.getOrDefault(roomNumber, Map.of()) : Map.of();

        SeatMapResponse res = new SeatMapResponse();
        res.classId = classId;
        res.roomNumber = roomNumber;
        res.map = map;
        return ResponseEntity.ok(res);
    }

    @PatchMapping("/{classId}/seatmap")
    public ResponseEntity<?> patchSeat(
            @PathVariable String classId,
            @RequestBody AssignSeatRequest body
    ) {
        if (body == null || body.roomNumber == null || body.seatLabel == null || body.seatLabel.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message","roomNumber and seatLabel are required"));
        }

        Course c = courseRepo.findByClassId(classId).orElse(null);
        if (c == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message","class not found"));
        }

        if (c.getSeatMap() == null) c.setSeatMap(new HashMap<>());
        c.getSeatMap().computeIfAbsent(body.roomNumber, k -> new HashMap<>());
        Map<String,String> roomMap = c.getSeatMap().get(body.roomNumber);

        String sid = (body.studentId == null || body.studentId.isBlank()) ? null : body.studentId;

        if (sid == null) {
            // 해제
            roomMap.remove(body.seatLabel);
        } else {
            // 동일 학생 중복 배정 방지(같은 room 내)
            roomMap.entrySet().removeIf(e -> sid.equals(e.getValue()));
            roomMap.put(body.seatLabel, sid);
        }

        courseRepo.save(c);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{classId}/seatmap")
    public ResponseEntity<?> putSeatMap(
            @PathVariable String classId,
            @RequestBody PutSeatMapRequest body
    ) {
        if (body == null || body.roomNumber == null) {
            return ResponseEntity.badRequest().body(Map.of("message","roomNumber is required"));
        }

        Course c = courseRepo.findByClassId(classId).orElse(null);
        if (c == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message","class not found"));
        }

        if (c.getSeatMap() == null) c.setSeatMap(new HashMap<>());

        Map<String,String> newMap = new LinkedHashMap<>();
        if (body.map != null) {
            // 마지막 값 우선 정책(중복 studentId 처리)
            Set<String> used = new HashSet<>();
            for (Map.Entry<String,String> e : body.map.entrySet()) {
                String label = e.getKey();
                String sid   = e.getValue();
                if (label == null || label.isBlank() || sid == null || sid.isBlank()) continue;
                used.add(sid);
                newMap.put(label, sid);
            }
        }

        c.getSeatMap().put(body.roomNumber, newMap);
        courseRepo.save(c);
        return ResponseEntity.ok().build();
    }
}
