// src/main/java/com/team103/service/DirectorSeatOverviewService.java
package com.team103.service;

import com.team103.dto.DirectorOverviewResponse;
import com.team103.dto.SeatBoardResponse;
import com.team103.model.Course;
import com.team103.model.Room;
import com.team103.model.WaitingRoom;
import com.team103.repository.CourseRepository;
import com.team103.repository.RoomRepository;
import com.team103.repository.StudentRepository;
import com.team103.repository.WaitingRoomRepository;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DirectorSeatOverviewService {

    private final RoomRepository roomRepo;
    private final CourseRepository courseRepo;
    private final WaitingRoomRepository waitingRepo;
    private final StudentRepository studentRepo;
    private final SeatBoardService seatSvc;

    public DirectorSeatOverviewService(RoomRepository roomRepo,
                                       CourseRepository courseRepo,
                                       WaitingRoomRepository waitingRepo,
                                       StudentRepository studentRepo,
                                       SeatBoardService seatSvc) {
        this.roomRepo = roomRepo;
        this.courseRepo = courseRepo;
        this.waitingRepo = waitingRepo;
        this.studentRepo = studentRepo;
        this.seatSvc = seatSvc;
    }

    /** 학원 전체 강의실별 좌석/출석 현황 + 웨이팅룸 */
    public DirectorOverviewResponse getAcademyOverview(int academyNumber, String date) {
        final String ymd = (date == null || date.isBlank()) ? SeatBoardService.todayYmd() : date.trim();

        // 1) 학원 강의실 목록
        final List<Room> rooms = roomRepo.findByAcademyNumber(academyNumber);

        // 2) 오늘(ymd) 사용 중인 반과 매칭된 좌석판을 구한다
        final List<DirectorOverviewResponse.RoomStatus> roomViews = new ArrayList<>();

        // Course 조회 (findByAcademyNumber 없으면 findAll로 대체)
        List<Course> courses;
        try {
            // 메서드가 있는 경우 (정상 시나리오)
            Method m = courseRepo.getClass().getMethod("findByAcademyNumber", int.class);
            @SuppressWarnings("unchecked")
            List<Course> tmp = (List<Course>) m.invoke(courseRepo, academyNumber);
            courses = tmp != null ? tmp : Collections.emptyList();
        } catch (Exception ignore) {
            // 없으면 전체 조회 후 필터
            courses = courseRepo.findAll().stream()
                    .filter(c -> {
                        Integer an = parseIntOrNull(getString(c, "getAcademyNumber"));
                        return an != null && an == academyNumber;
                    })
                    .collect(Collectors.toList());
        }

        for (Room room : rooms) {
            // 이 날짜에 이 강의실을 쓰는 반 찾기
            Optional<Course> usingCourse = courses.stream().filter(c -> {
                Object rf = call(c, "getRoomFor", new Class[]{String.class}, new Object[]{ymd});
                if (rf == null) return false;
                Integer rn = parseIntOrNull(String.valueOf(rf));
                return Objects.equals(rn, room.getRoomNumber());
            }).findFirst();

            SeatBoardResponse seatBoard;
            String className = null;

            if (usingCourse.isPresent()) {
                Course c = usingCourse.get();
                className = coalesce(getString(c, "getClassName"), getString(c, "getName"));
                String classId = coalesce(getString(c, "getClassId"), getString(c, "getId"));
                // 반이 있으면 반 기준 좌석판
                seatBoard = seatSvc.getSeatBoard(classId, ymd);
            } else {
                // 반이 없으면 강의실 레이아웃으로 빈 좌석판
                seatBoard = buildEmptyBoardFromRoom(room, ymd);
            }

            DirectorOverviewResponse.RoomStatus rs = new DirectorOverviewResponse.RoomStatus();
            rs.setRoomNumber(room.getRoomNumber());
            rs.setClassName(className);
            rs.setSeats(seatBoard.getSeats());
            rs.setPresentCount(seatBoard.getPresentCount());
            rs.setLateCount(seatBoard.getLateCount());
            rs.setAbsentCount(seatBoard.getAbsentCount());
            rs.setMoveOrBreakCount(seatBoard.getMoveOrBreakCount());
            rs.setNotRecordedCount(seatBoard.getNotRecordedCount());

            // (선택) 레이아웃 힌트
            roomViews.add(rs);
        }

        // 3) 웨이팅룸 -> 이름 매핑
        final List<WaitingRoom> waits = waitingRepo.findByAcademyNumber(academyNumber);
        final List<String> waitIds = waits.stream()
                .map(WaitingRoom::getStudentId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        final Map<String, String> nameById = studentRepo.findByStudentIdIn(waitIds).stream()
                .collect(Collectors.toMap(
                        s -> getString(s, "getStudentId"),
                        s -> coalesce(getString(s, "getStudentName"), getString(s, "getName")),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        final List<SeatBoardResponse.WaitingItem> waiting = waits.stream().map(w -> {
            SeatBoardResponse.WaitingItem wi = new SeatBoardResponse.WaitingItem();
            wi.setStudentId(w.getStudentId());
            wi.setStudentName(nameById.get(w.getStudentId()));
            wi.setStatus(w.getStatus());
            wi.setCheckedInAt(w.getCheckedInAt());
            return wi;
        }).collect(Collectors.toList());

        // 4) 응답
        DirectorOverviewResponse r = new DirectorOverviewResponse();
        r.setDate(ymd);
        r.setRooms(roomViews);
        r.setWaiting(waiting);
        return r;
    }

    /** 반 배정이 없을 때, Room 레이아웃만으로 빈 좌석판 생성 */
    static SeatBoardResponse buildEmptyBoardFromRoom(Room room, String ymd) {
        SeatBoardResponse r = new SeatBoardResponse();
        r.setDate(ymd);

        // vector(V2 -> V1) 우선
        List<Room.VectorSeat> vec = null;
        try { vec = castVectorSeatList(Room.class.getMethod("getVectorLayoutV2").invoke(room)); } catch (Exception ignore) {}
        if (vec == null || vec.isEmpty()) {
            try { vec = castVectorSeatList(Room.class.getMethod("getVectorLayout").invoke(room)); } catch (Exception ignore) {}
        }

        if (vec != null && !vec.isEmpty()) {
            r.setLayoutType("vector");
            r.setCanvasW(room.getVectorCanvasW() != null ? room.getVectorCanvasW() : 1.0);
            r.setCanvasH(room.getVectorCanvasH() != null ? room.getVectorCanvasH() : 1.0);
            r.setRows(null);
            r.setCols(null);

            List<SeatBoardResponse.SeatStatus> seats = new ArrayList<>();
            vec.stream()
               .sorted(Comparator.comparingInt(DirectorSeatOverviewService::seatOrderOfLocal))
               .forEach(v -> {
                   SeatBoardResponse.SeatStatus s = new SeatBoardResponse.SeatStatus();
                   Integer num = parseIntOrNull(v.getLabel());
                   s.setSeatNumber(num);
                   s.setRow(null);
                   s.setCol(null);
                   s.setX(v.getX()); s.setY(v.getY()); s.setW(v.getW()); s.setH(v.getH()); s.setR(v.getR());
                   s.setDisabled(Boolean.TRUE.equals(v.getDisabled()));
                   s.setAttendanceStatus("미기록");
                   seats.add(s);
               });
            r.setSeats(seats);
        } else {
            // grid
            r.setLayoutType("grid");
            r.setRows(room.getRows());
            r.setCols(room.getCols());
            r.setCanvasW(null);
            r.setCanvasH(null);

            List<SeatBoardResponse.SeatStatus> seats = new ArrayList<>();
            if (room.getLayout() != null) {
                room.getLayout().stream()
                    .sorted(Comparator.comparingInt(c -> c.getSeatNumber() == null ? 9999 : c.getSeatNumber()))
                    .forEach(c -> {
                        SeatBoardResponse.SeatStatus s = new SeatBoardResponse.SeatStatus();
                        s.setSeatNumber(c.getSeatNumber());
                        s.setRow(c.getRow());
                        s.setCol(c.getCol());
                        s.setX(null); s.setY(null); s.setW(null); s.setH(null); s.setR(null);
                        s.setDisabled(Boolean.TRUE.equals(c.getDisabled()));
                        s.setAttendanceStatus("미기록");
                        seats.add(s);
                    });
            }
            r.setSeats(seats);
        }

        r.setPresentCount(0);
        r.setLateCount(0);
        r.setAbsentCount(0);
        r.setMoveOrBreakCount(0);
        r.setNotRecordedCount(r.getSeats() != null ? r.getSeats().size() : 0);
        return r;
    }

    /* ---------------- helpers ---------------- */
    private static Object call(Object target, String name, Class<?>[] types, Object[] args) {
        if (target == null) return null;
        try {
            Method m = (types == null) ? target.getClass().getMethod(name)
                    : target.getClass().getMethod(name, types);
            m.setAccessible(true);
            return (args == null) ? m.invoke(target) : m.invoke(target, args);
        } catch (Exception e) {
            return null;
        }
    }

    private static String getString(Object target, String name) {
        Object v = call(target, name, null, null);
        return v == null ? null : String.valueOf(v);
    }

    private static String coalesce(String a, String b) {
        return (a != null && !a.isBlank()) ? a : b;
    }

    private static Integer parseIntOrNull(String s) {
        if (s == null) return null;
        try { return Integer.valueOf(s.replaceAll("[^0-9]", "")); }
        catch (Exception e) { return null; }
    }

    @SuppressWarnings("unchecked")
    private static List<Room.VectorSeat> castVectorSeatList(Object o) {
        try { return (List<Room.VectorSeat>) o; } catch (Exception e) { return null; }
    }

    private static int seatOrderOfLocal(Room.VectorSeat v) {
        Integer n = parseIntOrNull(v == null ? null : v.getLabel());
        return n == null ? Integer.MAX_VALUE : n;
    }
}
