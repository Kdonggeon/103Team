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

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class SeatBoardService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyy-MM-dd");

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

    /** 오늘 yyyy-MM-dd */
    public static String todayYmd() {
        return LocalDate.now(KST).format(YMD);
    }

    /** 좌석판 조회(수업+날짜) */
    public SeatBoardResponse getSeatBoard(String classId, String date) {
        String ymd = (date == null || date.isBlank()) ? todayYmd() : date;

        // 1) 수업
        Course course = courseRepo.findByClassId(classId)
                .orElseThrow(() -> new RuntimeException("class not found: " + classId));

        // 날짜별 오버라이드 우선 → 없으면 1순위 강의실
        Integer roomNumber = course.getRoomFor(ymd);
        List<Integer> academies = course.getAcademyNumbersSafe();
        Integer academyNumber = academies.isEmpty() ? course.getAcademyNumber() : academies.get(0);

        if (roomNumber == null || academyNumber == null) {
            throw new RuntimeException("room/academy is not set on course: " + classId);
        }

        // 2) 강의실
        Room room = roomRepo.findByRoomNumberAndAcademyNumber(roomNumber, academyNumber)
                .orElseThrow(() -> new RuntimeException("room not found: " + roomNumber + " (academy=" + academyNumber + ")"));

        // 3) 출석 문서
        Attendance att = attRepo.findFirstByClassIdAndDate(classId, ymd);
        if (att == null) {
            List<Attendance> list = attRepo.findByClassIdAndDate(classId, ymd);
            att = (list != null && !list.isEmpty()) ? list.get(0) : null;
        }

        // 4) 학생별 출석 상태 맵
        Map<String, String> statusByStudent = new HashMap<>();
        if (att != null && att.getAttendanceList() != null) {
            for (Object e : att.getAttendanceList()) {
                if (e == null) continue;
                if (e instanceof Map<?, ?> m) {
                    Object sid = m.get("Student_ID");
                    Object st = m.get("Status");
                    if (sid != null) statusByStudent.put(String.valueOf(sid), (st != null) ? String.valueOf(st) : "미기록");
                } else {
                    // POJO 형태 호환
                    try {
                        Object sid = e.getClass().getMethod("getStudentId").invoke(e);
                        Object st  = e.getClass().getMethod("getStatus").invoke(e);
                        if (sid != null) statusByStudent.put(String.valueOf(sid), (st != null) ? String.valueOf(st) : "미기록");
                    } catch (Exception ignore) {}
                }
            }
        }

        // 5) 좌석배정 매핑 (Seat_Assignments → seatLabel -> studentId)
        Map<String, String> studentBySeatLabel = new HashMap<>();
        if (att != null && att.getSeatAssignments() != null) {
            for (Attendance.SeatAssign a : att.getSeatAssignments()) {
                if (a == null) continue;
                String key = firstNonBlank(a.getSeatLabel(), a.getSeatId());
                if (key != null && a.getStudentId() != null) {
                    studentBySeatLabel.put(key, a.getStudentId());
                }
            }
        }

        // 6) 좌석 상태 생성 (강의실 레이아웃 기준으로 정렬/구성)
        List<SeatBoardResponse.SeatStatus> seatStatuses = new ArrayList<>();
        if (room.getLayout() != null) {
            room.getLayout().stream()
                .sorted(Comparator.comparing(Room.SeatCell::getSeatNumber, Comparator.nullsLast(Integer::compareTo)))
                .forEach(c -> {
                    SeatBoardResponse.SeatStatus s = new SeatBoardResponse.SeatStatus();
                    s.setSeatNumber(c.getSeatNumber());
                    s.setRow(c.getRow());
                    s.setCol(c.getCol());
                    s.setDisabled(Boolean.TRUE.equals(c.getDisabled()));

                    String label = (c.getSeatNumber() != null) ? String.valueOf(c.getSeatNumber()) : null;
                    String sid = (label != null) ? studentBySeatLabel.get(label) : null;
                    s.setStudentId(sid);
                    s.setAttendanceStatus((sid != null) ? statusByStudent.getOrDefault(sid, "미기록") : "미기록");
                    seatStatuses.add(s);
                });
        }

        // 7) 응답
        SeatBoardResponse res = new SeatBoardResponse();
        SeatBoardResponse.CurrentClass cc = new SeatBoardResponse.CurrentClass();
        cc.setClassId(course.getClassId());
        cc.setClassName(course.getClassName());
        res.setCurrentClass(cc);
        res.setRows(nvl(room.getRows(), 0));
        res.setCols(nvl(room.getCols(), 0));
        res.setSeats(seatStatuses);
        return res;
    }

    /** 좌석 배정(단건): seatLabel(보통 숫자) ↔ student 유일성 보장 후 저장 */
    public void assignSeat(String classId, String date, String seatLabel, String studentId) {
        if (seatLabel == null || seatLabel.isBlank()) throw new IllegalArgumentException("seatLabel required");
        if (studentId == null || studentId.isBlank()) throw new IllegalArgumentException("studentId required");
        String ymd = (date == null || date.isBlank()) ? todayYmd() : date;

        Attendance att = attRepo.findFirstByClassIdAndDate(classId, ymd);
        if (att == null) {
            att = new Attendance();
            att.setClassId(classId);
            att.setDate(ymd);
        }
        List<Attendance.SeatAssign> list = (att.getSeatAssignments() != null)
                ? new ArrayList<>(att.getSeatAssignments()) : new ArrayList<>();

        // 동일 좌석/동일 학생 중복 제거
        list.removeIf(x ->
                (x.getSeatLabel() != null && x.getSeatLabel().equals(seatLabel))
             || (x.getStudentId() != null && x.getStudentId().equals(studentId)));

        Attendance.SeatAssign a = new Attendance.SeatAssign();
        a.setSeatLabel(seatLabel);
        a.setStudentId(studentId);
        list.add(a);

        att.setSeatAssignments(list);
        attRepo.save(att);
    }

    /** 좌석 배정 해제 */
    public void unassignSeat(String classId, String date, String seatLabel) {
        String ymd = (date == null || date.isBlank()) ? todayYmd() : date;
        Attendance att = attRepo.findFirstByClassIdAndDate(classId, ymd);
        if (att == null || att.getSeatAssignments() == null) return;

        List<Attendance.SeatAssign> list = new ArrayList<>(att.getSeatAssignments());
        list.removeIf(x -> seatLabel.equals(x.getSeatLabel()));
        att.setSeatAssignments(list);
        attRepo.save(att);
    }

    /* -------- helpers -------- */

    private static <T> T nvl(T v, T d) { return (v != null) ? v : d; }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return null;
    }
}
