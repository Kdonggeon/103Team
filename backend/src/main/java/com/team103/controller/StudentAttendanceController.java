package com.team103.controller;

import com.team103.dto.StudentClassLiteDto;
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
     */
    @GetMapping("/{studentId}/classes")
    public List<StudentClassLiteDto> getMyClasses(@PathVariable String studentId) {
        List<Course> courses = courseRepo.findByStudentsContaining(studentId);
        List<StudentClassLiteDto> result = new ArrayList<>();

        // 기준 날짜: 일단 "오늘" 기준으로 overrides를 해석
        String today = LocalDate.now().toString(); // "YYYY-MM-DD"

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

            // (a) 시간 보정
            if (!hasBaseTime) {
                // 1순위: 오늘(today)에 대한 오버라이드
                DailyTime dtToday = c.getTimeFor(today);
                if (dtToday != null && dtToday.getStart() != null && dtToday.getEnd() != null) {
                    startTime = dtToday.getStart();
                    endTime   = dtToday.getEnd();
                } else if (extraDates != null && !extraDates.isEmpty()) {
                    // 2순위: Extra_Dates 중 하나에 대해 getTimeFor 호출
                    for (String d : extraDates) {
                        DailyTime dt = c.getTimeFor(d);
                        if (dt != null && dt.getStart() != null && dt.getEnd() != null) {
                            startTime = dt.getStart();
                            endTime   = dt.getEnd();
                            break;
                        }
                    }
                }
            }

            // (b) 요일 보정
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

    // (참고) 출석 기록 조회는 AttendanceController가 담당
}
