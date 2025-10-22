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
        // 1) 수업 로드
        Course course = courseRepo.findByClassId(classId)
                .orElseThrow(() -> new RuntimeException("class not found: " + classId));

        if (course.getRoomNumber() == null || course.getAcademyNumber() == null) {
            throw new RuntimeException("room/academy is not set on course: " + classId);
        }

        // 2) 강의실 로드
        Room room = roomRepo.findByRoomNumberAndAcademyNumber(
                course.getRoomNumber(), course.getAcademyNumber()
        ).orElseThrow(() ->
                new RuntimeException("room not found: " + course.getRoomNumber() +
                        " (academy=" + course.getAcademyNumber() + ")"));

        // 3) 출석 로드
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
                    // POJO 형태까지 호환
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

        // 5) seatNumber -> Room.SeatCell 매핑
        Map<Integer, Room.SeatCell> cellBySeat = new HashMap<>();
        if (room.getLayout() != null) {
            for (Room.SeatCell c : room.getLayout()) {
                cellBySeat.put(c.getSeatNumber(), c);
            }
        }

        // 6) 내려줄 좌석 상태
        List<SeatBoardResponse.SeatStatus> seatStatuses = new ArrayList<>();

        // (A) Course에 좌석 매핑이 있을 때(현재 Course엔 없음 → 보통 B로 감)
        try {
            var m = course.getClass().getMethod("getSeats");
            Object seatsObj = m.invoke(course);
            if (seatsObj instanceof Collection<?> seats) {
                for (Object cs : seats) {
                    Integer sn = readInt(cs, "getSeatNumber");
                    String sid  = readStr(cs, "getStudentId");

                    Room.SeatCell cell = (sn != null) ? cellBySeat.get(sn) : null;

                    SeatBoardResponse.SeatStatus s = new SeatBoardResponse.SeatStatus();
                    if (sn != null) s.setSeatNumber(sn);
                    if (cell != null) {
                        s.setRow(cell.getRow());
                        s.setCol(cell.getCol());
                        s.setDisabled(Boolean.TRUE.equals(cell.getDisabled()));
                    }
                    s.setStudentId(sid);
                    s.setAttendanceStatus(
                            (sid != null) ? statusByStudent.getOrDefault(sid, "미기록") : "미기록"
                    );
                    seatStatuses.add(s);
                }
            } else {
                fillFromRoomLayoutOnly(room, seatStatuses);
            }
        } catch (NoSuchMethodException nsme) {
            fillFromRoomLayoutOnly(room, seatStatuses);
        } catch (Exception ex) {
            fillFromRoomLayoutOnly(room, seatStatuses);
        }

        // 7) 응답
        SeatBoardResponse res = new SeatBoardResponse();

        SeatBoardResponse.CurrentClass cc = new SeatBoardResponse.CurrentClass();
        cc.setClassId(course.getClassId());
        cc.setClassName(course.getClassName());
        res.setCurrentClass(cc);

        res.setRows(room.getRows());
        res.setCols(room.getCols());
        res.setSeats(seatStatuses);
        return res;
    }

    /* -------- helpers -------- */

    private static void fillFromRoomLayoutOnly(Room room, List<SeatBoardResponse.SeatStatus> out) {
        if (room.getLayout() == null) return;
        for (Room.SeatCell c : room.getLayout()) {
            SeatBoardResponse.SeatStatus s = new SeatBoardResponse.SeatStatus();
            s.setSeatNumber(c.getSeatNumber());
            s.setRow(c.getRow());
            s.setCol(c.getCol());
            s.setDisabled(Boolean.TRUE.equals(c.getDisabled()));
            out.add(s);
        }
    }

    private static Integer readInt(Object obj, String getter) {
        try { Object v = obj.getClass().getMethod(getter).invoke(obj);
            return (v instanceof Number) ? ((Number) v).intValue() : null;
        } catch (Exception e) { return null; }
    }
    private static String readStr(Object obj, String getter) {
        try { Object v = obj.getClass().getMethod(getter).invoke(obj);
            return (v != null) ? String.valueOf(v) : null;
        } catch (Exception e) { return null; }
    }
}
