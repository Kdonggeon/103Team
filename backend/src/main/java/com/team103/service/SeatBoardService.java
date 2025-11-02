// src/main/java/com/team103/service/SeatBoardService.java
package com.team103.service;

import com.team103.dto.SeatBoardResponse;
import com.team103.model.Attendance;
import com.team103.model.Course;
import com.team103.model.Room;
import com.team103.repository.AttendanceRepository;
import com.team103.repository.CourseRepository;
import com.team103.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SeatBoardService {

    private final CourseRepository courseRepo;
    private final AttendanceRepository attRepo;
    private final RoomRepository roomRepo;

    public SeatBoardService(
            CourseRepository courseRepo,
            AttendanceRepository attRepo,
            RoomRepository roomRepo
    ) {
        this.courseRepo = courseRepo;
        this.attRepo = attRepo;
        this.roomRepo = roomRepo;
    }

    public SeatBoardResponse getSeatBoard(String classId, String date) {
        // 1) 수업
        Course course = courseRepo.findByClassId(classId)
                .orElseThrow(() -> new RuntimeException("class not found: " + classId));
        if (course.getRoomNumber() == null || course.getAcademyNumber() == null) {
            throw new RuntimeException("room/academy is not set on course: " + classId);
        }

        // 2) 강의실
        Room room = roomRepo.findByRoomNumberAndAcademyNumber(
                course.getRoomNumber(), course.getAcademyNumber()
        ).orElseThrow(() ->
                new RuntimeException("room not found: " + course.getRoomNumber() +
                        " (academy=" + course.getAcademyNumber() + ")"));

        // 3) 출석(해당일)
        Attendance att = attRepo.findFirstByClassIdAndDate(classId, date);
        if (att == null) {
            List<Attendance> list = attRepo.findByClassIdAndDate(classId, date);
            att = (list != null && !list.isEmpty()) ? list.get(0) : null;
        }

        // 4) 학생별 출석 맵
        Map<String, String> statusByStudent = new HashMap<>();
        if (att != null && att.getAttendanceList() != null) {
            for (Object e : att.getAttendanceList()) {
                if (e == null) continue;
                if (e instanceof Map<?, ?> m) {
                    Object sid = m.get("Student_ID");
                    Object st  = m.get("Status");
                    if (sid != null) {
                        statusByStudent.put(String.valueOf(sid),
                                (st != null) ? String.valueOf(st) : "미기록");
                    }
                } else {
                    try {
                        Object sid = e.getClass().getMethod("getStudentId").invoke(e);
                        Object st  = e.getClass().getMethod("getStatus").invoke(e);
                        if (sid != null) {
                            statusByStudent.put(String.valueOf(sid),
                                    (st != null) ? String.valueOf(st) : "미기록");
                        }
                    } catch (Exception ignore) {}
                }
            }
        }

        // 5) 좌석 상태 구성 (벡터 우선 → 레거시 폴백)
        List<SeatBoardResponse.SeatStatus> seatStatuses = new ArrayList<>();

        if (room.getVectorLayout() != null && !room.getVectorLayout().isEmpty()) {
            fillFromVectorLayout(room, statusByStudent, seatStatuses);
        } else if (room.getLegacyGridLayout() != null && !room.getLegacyGridLayout().isEmpty()) {
            fillFromLegacyGrid(room, seatStatuses);
        } // else: 비어있으면 프론트에서 안내

        // 6) 응답
        SeatBoardResponse res = new SeatBoardResponse();

        SeatBoardResponse.CurrentClass cc = new SeatBoardResponse.CurrentClass();
        cc.setClassId(course.getClassId());
        cc.setClassName(course.getClassName());
        res.setCurrentClass(cc);

        // rows/cols는 primitive int 세터 → null 금지. 벡터 모드에서는 0으로.
        res.setRows(room.getRows() == null ? 0 : room.getRows());
        res.setCols(room.getCols() == null ? 0 : room.getCols());
        res.setSeats(seatStatuses);
        return res;
    }

    /* ---------- helpers ---------- */

    /** 벡터 좌석을 SeatBoardResponse로 변환 */
    private static void fillFromVectorLayout(
            Room room,
            Map<String, String> statusByStudent,
            List<SeatBoardResponse.SeatStatus> out
    ) {
        List<Room.VectorSeat> v = room.getVectorLayout();
        for (int i = 0; i < v.size(); i++) {
            Room.VectorSeat seat = v.get(i);

            SeatBoardResponse.SeatStatus s = new SeatBoardResponse.SeatStatus();

            // 좌석번호: label이 숫자면 사용, 아니면 index+1
            int seatNumber = inferSeatNumber(seat.getLabel(), i + 1);
            s.setSeatNumber(seatNumber);

            s.setDisabled(Boolean.TRUE.equals(seat.getDisabled()));

            // 학생/출석: VectorSeat에 studentId 필드가 없을 수 있으므로 리플렉션으로 안전 접근
            String sid = tryGetString(seat, "getStudentId");
            s.setStudentId(sid);
            s.setAttendanceStatus(
                    (sid != null) ? statusByStudent.getOrDefault(sid, "미기록") : "미기록"
            );

            // 벡터는 row/col 개념이 없음 → 0으로
            s.setRow(0);
            s.setCol(0);

            out.add(s);
        }
    }

    /** 레거시 SeatCell 기반 폴백 */
    private static void fillFromLegacyGrid(Room room, List<SeatBoardResponse.SeatStatus> out) {
        int idx = 0;
        for (Room.SeatCell c : room.getLegacyGridLayout()) {
            SeatBoardResponse.SeatStatus s = new SeatBoardResponse.SeatStatus();
            int seatNumber = (c.getSeatNumber() == null ? (idx + 1) : c.getSeatNumber());
            s.setSeatNumber(seatNumber);
            s.setRow(c.getRow() == null ? 0 : c.getRow());
            s.setCol(c.getCol() == null ? 0 : c.getCol());
            s.setDisabled(Boolean.TRUE.equals(c.getDisabled()));
            s.setStudentId(null);
            s.setAttendanceStatus("미기록");
            out.add(s);
            idx++;
        }
    }

    private static int inferSeatNumber(String label, int fallback) {
        if (label == null || label.isBlank()) return fallback;
        try { return Integer.parseInt(label.trim()); } catch (Exception ignore) { return fallback; }
    }

    private static String tryGetString(Object obj, String getter) {
        try {
            Object v = obj.getClass().getMethod(getter).invoke(obj);
            return (v != null) ? String.valueOf(v) : null;
        } catch (Exception e) {
            return null;
        }
    }
}
