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

    public SeatBoardService(CourseRepository courseRepo,
                            AttendanceRepository attRepo,
                            RoomRepository roomRepo) {
        this.courseRepo = courseRepo;
        this.attRepo = attRepo;
        this.roomRepo = roomRepo;
    }

    public SeatBoardResponse getSeatBoard(String classId, String date) {
        // Course 는 null 가능 시그니처
        Course course = courseRepo.findByClassId(classId);
        if (course == null) {
            throw new RuntimeException("class not found: " + classId);
        }
        if (course.getRoomNumber() == null || course.getAcademyNumber() == null) {
            throw new RuntimeException("room/academy is not set on course: " + classId);
        }

        Room room = roomRepo.findByRoomNumberAndAcademyNumber(
                course.getRoomNumber(), course.getAcademyNumber()
        ).orElseThrow(() -> new RuntimeException("room not found: " + course.getRoomNumber()));

        // 출석: 우선 한 건만 필요 → findFirst..., 없으면 목록에서 첫 번째 사용
        Attendance att = attRepo.findFirstByClassIdAndDate(classId, date);
        if (att == null) {
            List<Attendance> list = attRepo.findByClassIdAndDate(classId, date);
            att = (list != null && !list.isEmpty()) ? list.get(0) : null;
        }

        Map<String, String> statusByStudent = new HashMap<>();
        if (att != null && att.getAttendanceList() != null) {
            for (Object e : att.getAttendanceList()) {
                if (e == null) continue;

                // Map 형태/POJO 형태 모두 호환
                if (e instanceof Map) {
                    Object sid = ((Map<?, ?>) e).get("Student_ID");
                    Object st  = ((Map<?, ?>) e).get("Status");
                    if (sid != null) statusByStudent.put(String.valueOf(sid), (st != null) ? String.valueOf(st) : "미기록");
                } else {
                    try {
                        Object sid = e.getClass().getMethod("getStudentId").invoke(e);
                        Object st  = e.getClass().getMethod("getStatus").invoke(e);
                        if (sid != null) statusByStudent.put(String.valueOf(sid), (st != null) ? String.valueOf(st) : "미기록");
                    } catch (Exception ignore) {}
                }
            }
        }

        Map<Integer, Room.SeatCell> cellBySeat = new HashMap<>();
        if (room.getLayout() != null) {
            for (Room.SeatCell c : room.getLayout()) {
                cellBySeat.put(c.getSeatNumber(), c);
            }
        }

        List<SeatBoardResponse.SeatStatus> list = new ArrayList<>();
        if (course.getSeats() != null) {
            for (Room.Seat cs : course.getSeats()) {
                Room.SeatCell cell = cellBySeat.get(cs.getSeatNumber());

                SeatBoardResponse.SeatStatus s = new SeatBoardResponse.SeatStatus();
                s.setSeatNumber(cs.getSeatNumber());
                if (cell != null) {
                    s.setRow(cell.getRow());
                    s.setCol(cell.getCol());
                    s.setDisabled(Boolean.TRUE.equals(cell.getDisabled()));
                }
                s.setStudentId(cs.getStudentId());
                s.setAttendanceStatus(statusByStudent.getOrDefault(cs.getStudentId(), "미기록"));
                list.add(s);
            }
        }

        SeatBoardResponse res = new SeatBoardResponse();
        res.setCurrentClass(course.getCurrent());
        res.setRows(room.getRows());
        res.setCols(room.getCols());
        res.setSeats(list);
        return res;
    }
}
