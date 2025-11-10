// src/main/java/com/team103/service/AttendanceSeedService.java
package com.team103.service;

import com.team103.model.Attendance;
import com.team103.model.Course;
import com.team103.repository.AttendanceRepository;
import com.team103.repository.CourseRepository;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class AttendanceSeedService {

    private final AttendanceRepository attRepo;
    private final CourseRepository courseRepo;

    public AttendanceSeedService(AttendanceRepository attRepo, CourseRepository courseRepo) {
        this.attRepo = attRepo;
        this.courseRepo = courseRepo;
    }

    private static Object tryInvoke(Object target, String name, Class<?>[] types, Object[] args) {
        if (target == null) return null;
        try {
            Method m = (types == null) ? target.getClass().getMethod(name)
                                       : target.getClass().getMethod(name, types);
            m.setAccessible(true);
            return (args == null) ? m.invoke(target) : m.invoke(target, args);
        } catch (Exception ignore) { return null; }
    }

    /** 달력에서 일정 추가 시, 해당 날짜 Attendance를 즉시 생성(이미 있으면 반환) */
    public Attendance ensureAttendanceForDate(String classId, String ymd) {
        Attendance att = attRepo.findFirstByClassIdAndDate(classId, ymd);
        if (att != null) return att;

        // 새 문서
        att = new Attendance();
        att.setClassId(Objects.requireNonNull(classId));
        att.setDate(Objects.requireNonNull(ymd));
        att.setAttendanceList(new ArrayList<>());
        att.setSeatAssignments(new ArrayList<>());

        // 코스 roster로 시드
        Course c = courseRepo.findByClassId(classId).orElse(null);
        if (c != null) {
            @SuppressWarnings("unchecked")
            List<String> roster = (List<String>) tryInvoke(c, "getStudents", null, null);
            if (roster != null) {
                for (String sid : roster) {
                    if (sid == null || sid.isBlank()) continue;
                    Attendance.Item item = new Attendance.Item();
                    item.setStudentId(sid);
                    item.setStatus("미기록");
                    att.getAttendanceList().add(item);
                }
            }
        }
        return attRepo.save(att);
    }

    /** 여러 날짜를 한 번에 생성 */
    public List<Attendance> ensureAttendanceForDates(String classId, List<String> ymdList) {
        List<Attendance> out = new ArrayList<>();
        if (ymdList == null) return out;
        for (String ymd : ymdList) {
            if (ymd == null || ymd.isBlank()) continue;
            out.add(ensureAttendanceForDate(classId, ymd));
        }
        return out;
    }
}
