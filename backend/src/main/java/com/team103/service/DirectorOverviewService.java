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

    public List<DirectorRoomView> getAcademyRoomsOverview(int academyNumber, String ymd) {
        final String date = (ymd == null || ymd.isBlank()) ? SeatBoardService.todayYmd() : ymd.trim();

        // 학원 내 모든 강의실
        List<Room> rooms = roomRepo.findByAcademyNumber(academyNumber);
        List<DirectorRoomView> out = new ArrayList<>();

        for (Room room : rooms) {
            // 해당 날짜에 이 방을 사용하는 반 탐색
            String classId = courseRepo.findByAcademyNumber(academyNumber).stream()
                    .filter(c -> {
                        Object rf = call(c, "getRoomFor",
                                new Class[]{String.class}, new Object[]{date});
                        if (rf == null) return false;
                        Integer rn = Integer.valueOf(String.valueOf(rf));
                        return Objects.equals(rn, room.getRoomNumber());
                    })
                    .map(c -> getString(c, "getClassId"))
                    .filter(s -> s != null && !s.isBlank())
                    .findFirst()
                    .orElse(null);

            // 좌석판
            SeatBoardResponse sb = (classId != null)
                    ? seatSvc.getSeatBoard(classId, date)
                    : DirectorSeatOverviewService.buildEmptyBoardFromRoom(room, date);

            // 반 이름
            String className = null;
            if (classId != null) {
                className = courseRepo.findByClassId(classId)
                        .map(c -> getString(c, "getClassName"))
                        .orElse(null);
            }

            DirectorRoomView v = new DirectorRoomView();
            v.setRoomNumber(room.getRoomNumber());
            v.setClassName(className);
            v.setSeatBoard(sb);
            out.add(v);
        }
        return out;
    }

    /* ------------ local reflection helpers (SeatBoardService에 의존 X) ------------ */
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
}
