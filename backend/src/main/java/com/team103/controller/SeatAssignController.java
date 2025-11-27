// src/main/java/com/team103/controller/SeatAssignController.java
package com.team103.controller;

import com.team103.dto.SeatAssignRequest;
import com.team103.dto.SeatBoardResponse;
import com.team103.model.Course;
import com.team103.repository.CourseRepository;
import com.team103.service.SeatBoardService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/seat")
@CrossOrigin(origins = "*")
public class SeatAssignController {

    private final SeatBoardService seatBoardService;
    private final CourseRepository courseRepo;

    public SeatAssignController(SeatBoardService seatBoardService,
                                CourseRepository courseRepo) {
        this.seatBoardService = seatBoardService;
        this.courseRepo = courseRepo;
    }

    /* ================= 유틸 & 리플렉션 헬퍼 ================= */
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static boolean isBlank(String s){ return s==null || s.isBlank(); }
    private static String nowHm(){ return LocalTime.now(KST).toString().substring(0,5); }

    private static Object tryInvoke(Object target, String name, Class<?>[] types, Object[] args) {
        if (target == null) return null;
        try {
            Method m = (types == null) ? target.getClass().getMethod(name)
                    : target.getClass().getMethod(name, types);
            m.setAccessible(true);
            return (args == null) ? m.invoke(target) : m.invoke(target, args);
        } catch (Exception ignore) { return null; }
    }
    private static String tryGetString(Object target, String name) {
        Object v = tryInvoke(target, name, null, null);
        return v == null ? null : String.valueOf(v);
    }
    private static boolean within(String start, String end, String now) {
        return start != null && end != null && now.compareTo(start) >= 0 && now.compareTo(end) <= 0;
    }

    /** 컨트롤러 내부판정: academy + room + date(+time)으로 오늘 진행중 Course 찾기 */
    private Course resolveOngoingCourseInline(int academyNumber, int roomNumber, String ymd, String hmOpt) {
        final String hm = (hmOpt == null || hmOpt.isBlank()) ? nowHm() : hmOpt.trim();

        List<Course> all = new ArrayList<>();
        List<Course> a = courseRepo.findByRoomNumber(roomNumber);
        List<Course> b = courseRepo.findByRoomNumbersContaining(roomNumber);
        if (a != null) all.addAll(a);
        if (b != null) all.addAll(b);

        // 학원 매칭
        all = all.stream().filter(c -> {
            Integer an = null;
            try { an = (Integer) tryInvoke(c, "getAcademyNumber", null, null); } catch (Exception ignore) {}
            if (an != null && an == academyNumber) return true;
            try {
                @SuppressWarnings("unchecked")
                List<Integer> list = (List<Integer>) tryInvoke(c, "getAcademyNumbersSafe", null, null);
                return list != null && list.contains(academyNumber);
            } catch (Exception ignore) {}
            return false;
        }).collect(Collectors.toList());

        // 오늘 이 방을 쓰는지 + 시간겹침
        List<Course> today = new ArrayList<>();
        for (Course c : all) {
            Integer rnToday = null;
            try {
                Object v = tryInvoke(c, "getRoomFor", new Class[]{String.class}, new Object[]{ymd});
                if (v instanceof Integer rni) rnToday = rni;
                else if (v != null) rnToday = Integer.valueOf(String.valueOf(v));
            } catch (Exception ignore) {}
            if (rnToday == null || rnToday != roomNumber) continue;

            String start = null, end = null;
            Object sc = tryInvoke(c, "getScheduleFor", new Class[]{String.class}, new Object[]{ymd});
            if (sc != null) {
                start = tryGetString(sc, "getStartTime");
                end   = tryGetString(sc, "getEndTime");
            }
            if (start == null || end == null) {
                start = tryGetString(c, "getStartTime");
                end   = tryGetString(c, "getEndTime");
            }
            if (start == null || end == null || within(start, end, hm)) today.add(c);
        }

        if (today.isEmpty())
            throw new RuntimeException("ongoing class not found for room " + roomNumber + " @ " + ymd + " " + hm);

        if (today.size() == 1) return today.get(0);

        today.sort((x, y) -> {
            String xs = tryGetString(x, "getStartTime");
            String ys = tryGetString(y, "getStartTime");
            if (xs == null && ys == null) return 0;
            if (xs == null) return 1;
            if (ys == null) return -1;
            return Math.abs(hm.compareTo(xs)) - Math.abs(hm.compareTo(ys));
        });
        return today.get(0);
    }

    /* ===================== 배정/해제 ===================== */
    @PostMapping("/assign")
    public ResponseEntity<?> assign(@RequestBody SeatAssignRequest req) {
        try {
            if (isBlank(req.getStudentId()))
                return ResponseEntity.badRequest().body("studentId required");
            if (req.getAcademyNumber() <= 0 || req.getRoomNumber() <= 0)
                return ResponseEntity.badRequest().body("academyNumber/roomNumber required");
            if ((req.getUnassign() == null || !req.getUnassign()) && req.getSeatNumber() <= 0)
                return ResponseEntity.badRequest().body("seatNumber required");

            final String ymd = (isBlank(req.getDate())) ? SeatBoardService.todayYmd() : req.getDate().trim();

            String classId = req.getClassId();
            if (isBlank(classId)) {
                Course c = resolveOngoingCourseInline(req.getAcademyNumber(), req.getRoomNumber(), ymd, null);
                classId = c.getClassId();
            }

            String seatLabel = String.valueOf(req.getSeatNumber());
            if (Boolean.TRUE.equals(req.getUnassign())) {
                seatBoardService.unassignSeat(classId, ymd, seatLabel);
            } else {
                boolean markAttendance = !"manual".equalsIgnoreCase(req.getSource());
                seatBoardService.assignSeat(classId, ymd, seatLabel, req.getStudentId(), markAttendance);
            }
            SeatBoardResponse board = seatBoardService.getSeatBoard(classId, ymd);
            return ResponseEntity.ok(board);

        } catch (RuntimeException re) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(re.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("assign failed");
        }
    }

    /* ===================== 좌석판 조회 ===================== */
    @GetMapping("/board")
    public ResponseEntity<?> board(@RequestParam(required = false) String classId,
                                   @RequestParam(required = false) String date,
                                   @RequestParam(required = false) Integer academyNumber,
                                   @RequestParam(required = false) Integer roomNumber) {
        try {
            final String ymd = (date == null || date.isBlank()) ? SeatBoardService.todayYmd() : date.trim();

            String resolvedClassId = classId;
            if (isBlank(resolvedClassId)) {
                if (academyNumber == null || academyNumber <= 0 || roomNumber == null || roomNumber <= 0) {
                    return ResponseEntity.badRequest().body("academyNumber & roomNumber required when classId missing");
                }
                Course c = resolveOngoingCourseInline(academyNumber, roomNumber, ymd, null);
                resolvedClassId = c.getClassId();
            }

            SeatBoardResponse board = seatBoardService.getSeatBoard(resolvedClassId, ymd);
            return ResponseEntity.ok(board);

        } catch (RuntimeException re) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(re.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("board failed");
        }
    }
}
