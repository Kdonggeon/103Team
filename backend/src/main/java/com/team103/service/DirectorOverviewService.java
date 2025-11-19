// src/main/java/com/team103/service/DirectorOverviewService.java
package com.team103.service;

import com.team103.dto.DirectorRoomView;
import com.team103.dto.SeatBoardResponse;
import com.team103.model.Course;
import com.team103.model.Room;
import com.team103.repository.CourseRepository;
import com.team103.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class DirectorOverviewService {

    private final RoomRepository roomRepo;
    private final CourseRepository courseRepo;
    private final SeatBoardService seatSvc;

    public DirectorOverviewService(RoomRepository roomRepo,
                                   CourseRepository courseRepo,
                                   SeatBoardService seatSvc) {
        this.roomRepo = roomRepo;
        this.courseRepo = courseRepo;
        this.seatSvc = seatSvc;
    }

    /** 원장의 학원번호(academyNumber) 기준으로만 조회 */
    public List<DirectorRoomView> getAcademyRoomsOverview(int academyNumber, String ymd) {

        final String date = (ymd == null || ymd.isBlank())
                ? SeatBoardService.todayYmd()
                : ymd.trim();

        /* 1) 학원 내 모든 강의실 */
        List<Room> rooms = roomRepo.findByAcademyNumber(academyNumber);

        /* 2) 학원 내 모든 Course만 필터 */
        List<Course> courses = getCoursesByAcademy(academyNumber);

        List<DirectorRoomView> out = new ArrayList<>();

        for (Room room : rooms) {

            /* 3) 해당 날짜(date)에 이 방을 사용하는 반 찾기 */
            String classId = courses.stream()
                    .filter(c -> isRoomUsedOnDate(c, room.getRoomNumber(), date))
                    .map(c -> getString(c, "getClassId"))
                    .filter(id -> id != null && !id.isBlank())
                    .findFirst()
                    .orElse(null);

            /* 4) seatBoard 가져오기 (학원번호 필터 적용된 classId만 허용) */
            SeatBoardResponse sb;
            String className = null;

            if (classId != null) {

                // classId가 진짜 같은 학원인지 확인
                Integer courseAcademy = parseIntOrNull(getString(
                        findCourseById(courses, classId),
                        "getAcademyNumber"
                ));

                if (courseAcademy != null && courseAcademy == academyNumber) {
                    // 정상: 같은 학원
                    sb = seatSvc.getSeatBoard(classId, date);

                    /* className도 같은 학원 Course에서 조회 */
                    className = getString(findCourseById(courses, classId), "getClassName");

                } else {
                    // 다른 학원 classId면 무시해야 함
                    sb = DirectorSeatOverviewService.buildEmptyBoardFromRoom(room, date);
                }

            } else {
                // 반이 없는 경우
                sb = DirectorSeatOverviewService.buildEmptyBoardFromRoom(room, date);
            }

            /* 5) RoomView 구성 */
            DirectorRoomView v = new DirectorRoomView();
            v.setRoomNumber(room.getRoomNumber());
            v.setClassName(className);
            v.setSeatBoard(sb);
            out.add(v);
        }

        return out;
    }

    /* ================= 공통 유틸 함수 ================= */

    /** Reflection safe-call */
    private static Object call(Object target, String name, Class<?>[] types, Object[] args) {
        if (target == null) return null;
        try {
            Method m = (types == null)
                    ? target.getClass().getMethod(name)
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

    private static Integer parseIntOrNull(String s) {
        if (s == null) return null;
        try { return Integer.valueOf(s.replaceAll("[^0-9]", "")); }
        catch (Exception e) { return null; }
    }

    /** 해당 날짜(date)에 roomNumber를 사용하는 반인지 검사 */
    private static boolean isRoomUsedOnDate(Course c, int roomNumber, String date) {
        Object rf = call(c, "getRoomFor", new Class[]{String.class}, new Object[]{date});
        if (rf == null) return false;
        Integer rn = parseIntOrNull(String.valueOf(rf));
        return Objects.equals(rn, roomNumber);
    }

    /** 학원 내 Course 목록 가져오기 */
    private List<Course> getCoursesByAcademy(int academyNumber) {
        try {
            Method m = courseRepo.getClass().getMethod("findByAcademyNumber", int.class);
            @SuppressWarnings("unchecked")
            List<Course> list = (List<Course>) m.invoke(courseRepo, academyNumber);
            return list != null ? list : List.of();
        } catch (Exception ignore) {
            // fallback: findAll 후 필터
            List<Course> all = courseRepo.findAll();
            List<Course> out = new ArrayList<>();
            for (Course c : all) {
                Integer an = parseIntOrNull(getString(c, "getAcademyNumber"));
                if (an != null && an == academyNumber) out.add(c);
            }
            return out;
        }
    }

    /** classId 로 Course 찾아오기 */
    private Course findCourseById(List<Course> list, String classId) {
        for (Course c : list) {
            String id = getString(c, "getClassId");
            if (classId.equals(id)) return c;
        }
        return null;
    }
}
