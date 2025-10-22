// src/main/java/com/team103/controller/TeacherScheduleController.java
package com.team103.controller;

import com.team103.model.Course;
import com.team103.repository.CourseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 별도 schedules 컬렉션 없이, Course(반) + Room 데이터만으로
 * "계산해서" 스케줄을 제공하는 컨트롤러.
 *
 * - day / range 조회는 현재 Course 필드(daysOfWeek, start/end, extraDates, cancelledDates, roomNumber…)만으로 계산.
 * - POST /schedules는 반복 요일이 아닌 "임시 수업"을 extraDates에 추가하는 용도로만 사용(옵션).
 */
@RestController
@RequestMapping("/api/manage/teachers")
public class TeacherScheduleController {

    private final CourseRepository courseRepo;

    public TeacherScheduleController(CourseRepository courseRepo) {
        this.courseRepo = courseRepo;
    }

    /* ───────── 공통 가드 ───────── */

    private boolean hasRole(Authentication auth, String role) {
        if (auth == null) return false;
        String needed = "ROLE_" + role;
        return auth.getAuthorities().stream().anyMatch(a -> needed.equals(a.getAuthority()));
    }

    private void guardTeacherId(String teacherId, Authentication auth) {
        String me = (auth != null) ? auth.getName() : null;
        boolean director = hasRole(auth, "DIRECTOR");
        if (me == null || (!director && !teacherId.equals(me))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "teacherId mismatch");
        }
    }

    /* ───────── DTO (프론트 api.ts 에 맞춤) ───────── */

    public record ScheduleItem(
        String scheduleId,  // 계산형이라 classId+date로 임시 생성
        String teacherId,
        String date,        // YYYY-MM-DD
        String classId,
        String title,       // className 사용
        String startTime,   // HH:mm
        String endTime,     // HH:mm
        Integer roomNumber,
        String memo
    ) {}

    public record CreateScheduleReq(
        String date,        // YYYY-MM-DD
        String classId,
        String title,       // 무시(반 이름으로 표기됨)
        String startTime,   // 무시(반의 startTime 사용)
        String endTime,     // 무시(반의 endTime 사용)
        Integer roomNumber, // 무시(반의 roomNumber 사용)
        String memo         // 무시(반의 schedule/memo 사용)
    ) {}

    /* ───────── 유틸 ───────── */

    private static int toMonFirstDow(LocalDate d) {
        // 1=Mon ... 7=Sun 로 변환
        DayOfWeek dw = d.getDayOfWeek();
        return (dw.getValue()); // ISO: Monday=1 ... Sunday=7
    }

    private static boolean isIn(String ymd, Collection<String> dates) {
        if (dates == null) return false;
        return dates.stream().filter(Objects::nonNull).anyMatch(ymd::equals);
    }

    private static String makeScheduleId(String classId, String ymd) {
        return classId + "_" + ymd;
    }

    private static ScheduleItem toItem(Course c, String ymd) {
        return new ScheduleItem(
            makeScheduleId(c.getClassId(), ymd),
            c.getTeacherId(),
            ymd,
            c.getClassId(),
            c.getClassName(),
            nullSafe(c.getStartTime()),
            nullSafe(c.getEndTime()),
            c.getRoomNumber(),
            c.getSchedule() // course-level memo
        );
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    /* ───────── 조회: 특정 일 ───────── */

    @GetMapping("/{teacherId}/schedules/day")
    public List<ScheduleItem> getDay(@PathVariable String teacherId,
                                     @RequestParam String date,
                                     Authentication auth) {
        guardTeacherId(teacherId, auth);
        if (date == null || date.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "date required (YYYY-MM-DD)");
        }
        LocalDate target = LocalDate.parse(date.strip());
        int dow = toMonFirstDow(target); // 1~7
        String ymd = target.toString();

        // 내 반 전체 로드 (repository에 teacherId 기준 조회 메서드가 있으면 교체)
        List<Course> myCourses = courseRepo.findByTeacherId(teacherId);

        List<ScheduleItem> out = new ArrayList<>();
        for (Course c : myCourses) {
            // 휴강 우선 배제
            if (isIn(ymd, c.getCancelledDates())) continue;

            boolean byWeekly = false;
            if (c.getDaysOfWeek() != null) {
                for (Object o : c.getDaysOfWeek()) {
                    int n;
                    if (o instanceof Number num) n = num.intValue();
                    else n = Integer.parseInt(String.valueOf(o));
                    if (n == dow) { byWeekly = true; break; }
                }
            }
            boolean byExtra = isIn(ymd, c.getExtraDates());

            if (byWeekly || byExtra) {
                out.add(toItem(c, ymd));
            }
        }
        // 시간순 정렬(옵션)
        return out.stream()
                .sorted(Comparator.comparing(ScheduleItem::startTime)
                        .thenComparing(ScheduleItem::classId))
                .collect(Collectors.toList());
    }

    /* ───────── 조회: 범위(from~to, to exclusive) ───────── */

    @GetMapping(value = "/{teacherId}/schedules", params = {"from","to"})
    public List<ScheduleItem> list(@PathVariable String teacherId,
                                   @RequestParam String from,
                                   @RequestParam String to,
                                   Authentication auth) {
        guardTeacherId(teacherId, auth);
        if (from == null || to == null || from.isBlank() || to.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from/to required");
        }
        LocalDate f = LocalDate.parse(from.strip());
        LocalDate t = LocalDate.parse(to.strip());

        List<Course> myCourses = courseRepo.findByTeacherId(teacherId);
        List<ScheduleItem> out = new ArrayList<>();

        for (LocalDate d = f; d.isBefore(t); d = d.plusDays(1)) {
            String ymd = d.toString();
            int dow = toMonFirstDow(d);

            for (Course c : myCourses) {
                if (isIn(ymd, c.getCancelledDates())) continue;

                boolean byWeekly = false;
                if (c.getDaysOfWeek() != null) {
                    for (Object o : c.getDaysOfWeek()) {
                        int n;
                        if (o instanceof Number num) n = num.intValue();
                        else n = Integer.parseInt(String.valueOf(o));
                        if (n == dow) { byWeekly = true; break; }
                    }
                }
                boolean byExtra = isIn(ymd, c.getExtraDates());

                if (byWeekly || byExtra) {
                    out.add(toItem(c, ymd));
                }
            }
        }
        return out.stream()
                .sorted(Comparator
                        .comparing(ScheduleItem::date)
                        .thenComparing(ScheduleItem::startTime)
                        .thenComparing(ScheduleItem::classId))
                .collect(Collectors.toList());
    }

    /* ───────── 생성(옵션): 반복 요일이 아닌 임시 수업을 extraDates에 추가 ───────── */

    @PostMapping("/{teacherId}/schedules")
    public ResponseEntity<?> create(@PathVariable String teacherId,
                                    @RequestBody CreateScheduleReq body,
                                    Authentication auth) {
        guardTeacherId(teacherId, auth);
        if (body == null || body.date() == null || body.date().isBlank()) {
            return ResponseEntity.badRequest().body("date required (YYYY-MM-DD)");
        }
        if (body.classId() == null || body.classId().isBlank()) {
            return ResponseEntity.badRequest().body("classId required");
        }

        Course c = courseRepo.findByClassId(body.classId()).orElse(null);
        if (c == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("class not found");

        // 소유자 체크 (원장은 예외)
        if (!(hasRole(auth, "DIRECTOR") || teacherId.equals(c.getTeacherId()))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("not your class");
        }

        LocalDate target = LocalDate.parse(body.date().strip());
        int dow = toMonFirstDow(target);
        boolean matchesWeekly = false;
        if (c.getDaysOfWeek() != null) {
            for (Object o : c.getDaysOfWeek()) {
                int n = (o instanceof Number num) ? num.intValue() : Integer.parseInt(String.valueOf(o));
                if (n == dow) { matchesWeekly = true; break; }
            }
        }

        // 이미 주 반복에 포함되는 날이면 별도 저장 없이 OK 반환
        if (matchesWeekly) {
            return ResponseEntity.ok(toItem(c, target.toString()));
        }

        // 반복이 아닌 날 → extraDates에 추가
        Set<String> extras = new LinkedHashSet<>(Optional.ofNullable(c.getExtraDates()).orElseGet(ArrayList::new));
        if (!extras.contains(target.toString())) {
            extras.add(target.toString());
            c.setExtraDates(new ArrayList<>(extras));
            courseRepo.save(c);
        }
        return ResponseEntity.ok(toItem(c, target.toString()));
    }
}
