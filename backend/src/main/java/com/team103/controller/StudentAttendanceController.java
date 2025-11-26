package com.team103.controller;

import com.team103.dto.StudentClassLiteDto;
import com.team103.dto.StudentClassSlotDto;
import com.team103.model.Course;
import com.team103.model.Course.DailyTime;
import com.team103.repository.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/students")
@CrossOrigin(origins = "*")
public class StudentAttendanceController {

    @Autowired
    private CourseRepository courseRepo;

    /**
     * 내가 수강하는 수업 조회
     * - 기존에는 Course 그대로 반환했지만,
     *   이제는 학생 시간표용으로 경량 DTO(StudentClassLiteDto)를 반환한다.
     * - Start_Time / End_Time / Days_Of_Week 이 없고
     *   Extra_Dates + Date_Time_Overrides만 있는 경우까지 보정한다.
     *
     * 변경점:
     * - "오늘(today)" 기준으로 Date_Time_Overrides를 잡지 않고
     *   Date_Time_Overrides / Extra_Dates 중에서 가장 이른 날짜를 기준으로
     *   대표 startTime / endTime 을 선택한다.
     *
     * → 이 엔드포인트는 기존처럼
     *   "한 수업 = 하나의 시간(start/end) + 여러 요일(daysOfWeek)" 구조를 유지한다.
     *   (웹 학생 시간표는 아래 /timetable 엔드포인트를 사용)
     */
    @GetMapping("/{studentId}/classes")
    public List<StudentClassLiteDto> getMyClasses(@PathVariable String studentId) {
        List<Course> courses = courseRepo.findByStudentsContaining(studentId);
        List<StudentClassLiteDto> result = new ArrayList<>();

        for (Course c : courses) {

            // ---------- 1) id / name ----------
            String id = (c.getClassId() != null && !c.getClassId().isBlank())
                    ? c.getClassId()
                    : c.getId();
            String name = c.getClassName();

            // ---------- 2) 기본 강의실/시간/요일 ----------
            Integer roomNumber = c.getPrimaryRoomNumber();    // Room_Numbers[0] 우선, 없으면 roomNumber
            String startTime = c.getStartTime();              // "HH:mm" (null 가능)
            String endTime   = c.getEndTime();                // "HH:mm" (null 가능)
            List<Integer> daysOfWeek = c.getDaysOfWeekInt();  // 1~7 (월=1 … 일=7), 없을 수 있음

            boolean hasBaseTime = startTime != null && !startTime.isBlank()
                                && endTime   != null && !endTime.isBlank();
            boolean hasBaseDays = daysOfWeek != null && !daysOfWeek.isEmpty();

            // ---------- 3) Extra_Dates + Overrides로 보정 ----------
            List<String> extraDates = c.getExtraDates();

            // (a) 시간 보정: today 사용 X,
            //     Date_Time_Overrides / Extra_Dates 중 "가장 이른 날짜"를 대표 시간으로 사용
            if (!hasBaseTime) {
                DailyTime picked = null;

                // 1순위: Date_Time_Overrides 중 가장 이른 날짜
                Map<String, DailyTime> overrides = c.getDateTimeOverrides();
                if (overrides != null && !overrides.isEmpty()) {
                    List<String> keys = new ArrayList<>(overrides.keySet());
                    Collections.sort(keys); // "2025-11-18" < "2025-11-20"
                    for (String k : keys) {
                        DailyTime dt = overrides.get(k);
                        if (dt != null && dt.getStart() != null && dt.getEnd() != null) {
                            picked = dt;
                            break;
                        }
                    }
                }

                // 2순위: Extra_Dates 중 가장 이른 날짜에 대해 getTimeFor 호출
                if (picked == null && extraDates != null && !extraDates.isEmpty()) {
                    List<String> dates = new ArrayList<>(extraDates);
                    Collections.sort(dates);
                    for (String d : dates) {
                        DailyTime dt = c.getTimeFor(d); // d = "YYYY-MM-DD"
                        if (dt != null && dt.getStart() != null && dt.getEnd() != null) {
                            picked = dt;
                            break;
                        }
                    }
                }

                if (picked != null) {
                    startTime = picked.getStart();
                    endTime   = picked.getEnd();
                    hasBaseTime = true;
                }
            }

            // (b) 요일 보정: Days_Of_Week가 없으면 Extra_Dates로 요일 계산
            if (!hasBaseDays && extraDates != null && !extraDates.isEmpty()) {
                Set<Integer> dowSet = new LinkedHashSet<>();
                for (String d : extraDates) {
                    try {
                        LocalDate ld = LocalDate.parse(d);         // "YYYY-MM-DD"
                        DayOfWeek dow = ld.getDayOfWeek();        // MONDAY=1 … SUNDAY=7
                        dowSet.add(dow.getValue());
                    } catch (Exception ignore) {
                    }
                }
                if (!dowSet.isEmpty()) {
                    daysOfWeek = new ArrayList<>(dowSet);         // 1~7 리스트
                    hasBaseDays = true;
                }
            }

            // ---------- 4) DTO로 변환 ----------
            StudentClassLiteDto dto = new StudentClassLiteDto();
            dto.setId(id);
            dto.setName(name);
            dto.setRoomNumber(roomNumber);
            dto.setStartTime(startTime);
            dto.setEndTime(endTime);
            dto.setDaysOfWeek(daysOfWeek);

            result.add(dto);
        }

        return result;
    }

    /**
     * 주간 시간표 (슬롯 단위)
     * - /api/students/{studentId}/timetable?weekStart=YYYY-MM-DD&days=7
     * - weekStart가 없으면, 요청 시점을 포함하는 주의 "월요일"부터 days일 간 계산
     * - 한 수업(Course)에 대해서도 날짜별로 slot을 쪼개서 반환한다.
     *
     * ⚠ 주간반(daysOfWeek) 패턴은 사용하지 않고,
     *   Extra_Dates (+ Cancelled_Dates, Overrides)만 기준으로 "실제 여는 날짜"를 계산한다.
     *   → 선생 시간표에서 날짜를 찍어서 만든 세션만 학생에게도 보이게 되는 구조.
     */
    @GetMapping("/{studentId}/timetable")
    public List<StudentClassSlotDto> getWeeklyTimetable(
            @PathVariable String studentId,
            @RequestParam(name = "weekStart", required = false) String weekStart,
            @RequestParam(name = "days", required = false, defaultValue = "7") int days
    ) {
        if (days <= 0) days = 7;

        // 1) 기준 주 시작일 계산 (월요일 기준)
        LocalDate base = LocalDate.now();
        DayOfWeek dow = base.getDayOfWeek(); // MONDAY=1 .. SUNDAY=7
        int shift = (dow == DayOfWeek.MONDAY)
                ? 0
                : (DayOfWeek.MONDAY.getValue() - dow.getValue());
        LocalDate defaultWeekStart = base.plusDays(shift);

        LocalDate startDate;
        try {
            startDate = (weekStart != null && !weekStart.isBlank())
                    ? LocalDate.parse(weekStart)
                    : defaultWeekStart;
        } catch (Exception e) {
            startDate = defaultWeekStart;
        }

        LocalDate endDate = startDate.plusDays(days - 1);

        // 2) 학생이 수강하는 수업 조회
        List<Course> courses = courseRepo.findByStudentsContaining(studentId);
        List<StudentClassSlotDto> slots = new ArrayList<>();

        for (Course c : courses) {
            String classId = (c.getClassId() != null && !c.getClassId().isBlank())
                    ? c.getClassId()
                    : c.getId();
            String className = c.getClassName();
            Integer baseRoom = c.getPrimaryRoomNumber();
            Integer academyNumber = c.getAcademyNumber();

            // "주간반" 패턴은 사용하지 않고, Extra_Dates만 사용
            List<String> extraDates = c.getExtraDates();
            List<String> cancelled = c.getCancelledDates();
            Map<String, Integer> dateRoomOverrides = c.getDateRoomOverrides();

            // 빠른 조회를 위해 set화
            Set<String> extraSet = new HashSet<>();
            if (extraDates != null) extraSet.addAll(extraDates);

            Set<String> cancelledSet = new HashSet<>();
            if (cancelled != null) cancelledSet.addAll(cancelled);

            for (int i = 0; i < days; i++) {
                LocalDate d = startDate.plusDays(i);
                if (d.isBefore(startDate) || d.isAfter(endDate)) continue;

                String ymd = d.toString(); // "YYYY-MM-DD"
                int dayValue = d.getDayOfWeek().getValue(); // 1~7

                // 2-1) 휴강 날짜면 스킵
                if (cancelledSet.contains(ymd)) {
                    continue;
                }

                // 2-2) 이 날짜에 수업이 있는지 판단
                //     → "주간반"은 안 쓰고, Extra_Dates 기준으로만 계산
                if (!extraSet.contains(ymd)) {
                    continue;
                }

                // 2-3) 시간: Date_Time_Overrides → 기본 Start/End_Time
                DailyTime dt = c.getTimeFor(ymd); // Date_Time_Overrides 우선, 없으면 기본 start/end
                String startTime = (dt != null) ? dt.getStart() : c.getStartTime();
                String endTime   = (dt != null) ? dt.getEnd()   : c.getEndTime();

                if ((startTime == null || startTime.isBlank())
                        && (endTime == null || endTime.isBlank())) {
                    // 시간 정보가 전혀 없으면 그 슬롯은 스킵
                    continue;
                }

                // 2-4) 강의실: 날짜별 오버라이드 → 기본 room
                Integer roomNumber = null;
                if (dateRoomOverrides != null && dateRoomOverrides.containsKey(ymd)) {
                    roomNumber = dateRoomOverrides.get(ymd);
                }
                if (roomNumber == null) {
                    roomNumber = baseRoom;
                }

                // 2-5) DTO 생성
                StudentClassSlotDto slot = new StudentClassSlotDto();
                slot.setClassId(classId);
                slot.setClassName(className);
                slot.setDate(ymd);
                // 이 dayOfWeek는 패턴이 아니라, 해당 date의 요일(캘린더 표시용)
                slot.setDayOfWeek(dayValue);  // 1~7
                slot.setRoomNumber(roomNumber);
                slot.setStartTime(startTime);
                slot.setEndTime(endTime);
                slot.setAcademyNumber(academyNumber);

                slots.add(slot);
            }
        }

        return slots;
    }

    // (참고) 출석 기록 조회는 AttendanceController가 담당
}
