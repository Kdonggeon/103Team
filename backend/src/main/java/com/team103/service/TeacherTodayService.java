// src/main/java/com/team103/service/TeacherTodayService.java
package com.team103.service;

import com.team103.dto.TeacherClassLite;
import com.team103.model.Course;
import com.team103.repository.CourseRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 교사용 대시보드 - 오늘 수업 목록
 * - 취소일/특별일/요일/오버라이드 반영
 * - 오늘 실제로 강의실을 사용하는 수업만 반환
 */
@Service
public class TeacherTodayService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final CourseRepository courseRepo;

    public TeacherTodayService(CourseRepository courseRepo) {
        this.courseRepo = Objects.requireNonNull(courseRepo);
    }

    /** 오늘 yyyy-MM-dd */
    public static String todayYmd() {
        return LocalDate.now(KST).format(YMD);
    }

    /**
     * 오늘 수업만 반환:
     * 1) 취소일이면 제외
     * 2) 특별일(Extra_Dates)이면 포함
     * 3) 아니면 요일 매칭(1..7) 포함
     * 4) 최종적으로 오늘 사용할 roomNumber가 있어야 포함
     * 5) 시간은 DailyTime 오버라이드 우선 → 기본 startTime/endTime
     */
    public List<TeacherClassLite> getTodayClasses(String teacherId, String dateOpt) {
        final String ymd = (dateOpt == null || dateOpt.isBlank()) ? todayYmd() : dateOpt.trim();
        final DayOfWeek dow = LocalDate.parse(ymd, YMD).getDayOfWeek();
        final int isoDow = dow.getValue(); // 1..7 (월=1 … 일=7)

        final List<Course> all = courseRepo.findByTeacherId(teacherId);
        final List<TeacherClassLite> out = new ArrayList<>();

        for (Course c : all) {
            // 0) 취소일이면 스킵
            if (c.getCancelledDates() != null && c.getCancelledDates().contains(ymd)) {
                continue;
            }

            // 1) 오늘 수업 여부: 특별일 우선, 아니면 요일 매칭
            boolean hasToday = false;

            if (c.getExtraDates() != null && c.getExtraDates().contains(ymd)) {
                hasToday = true;
            } else {
                List<Integer> dows = c.getDaysOfWeekInt(); // "１","월","2" 등 혼재 대비한 헬퍼
                if (dows != null && dows.contains(isoDow)) {
                    hasToday = true;
                }
            }

            if (!hasToday) continue;

            // 2) 오늘 사용할 강의실 번호 (오버라이드 우선)
            Integer roomNumberToday;
            try {
                roomNumberToday = c.getRoomFor(ymd); // 없으면 primaryRoom 또는 null
            } catch (Throwable t) {
                roomNumberToday = null;
            }
            if (roomNumberToday == null) continue; // 방이 없으면 오늘 수업 아님

            // 3) 시간: DailyTime 오버라이드 우선 → 기본 필드
            String start = null;
            String end   = null;
            try {
                Course.DailyTime dt = c.getTimeFor(ymd); // {start,end}
                if (dt != null) { start = dt.getStart(); end = dt.getEnd(); }
            } catch (Throwable ignored) {}
            if (start == null) start = c.getStartTime();
            if (end   == null) end   = c.getEndTime();

            out.add(
                TeacherClassLite.builder()
                    .classId(c.getClassId())
                    .className(c.getClassName())
                    .roomNumber(roomNumberToday)
                    .startTime(start)
                    .endTime(end)
                    .build()
            );
        }

        // 4) 시작 시간 기준 정렬(null은 뒤)
        out.sort((a, b) -> {
            String sa = a.getStartTime(), sb = b.getStartTime();
            if (sa == null && sb == null) return 0;
            if (sa == null) return 1;
            if (sb == null) return -1;
            return sa.compareTo(sb);
        });

        return out;
    }
}
