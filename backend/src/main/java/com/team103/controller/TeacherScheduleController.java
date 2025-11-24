// src/main/java/com/team103/controller/TeacherScheduleController.java
package com.team103.controller;

import com.team103.model.Course;
import com.team103.repository.CourseRepository;
import com.team103.service.AttendanceSeedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/manage/teachers")
public class TeacherScheduleController {

    private static final Logger log = LoggerFactory.getLogger(TeacherScheduleController.class);

    private final CourseRepository courseRepo;
    private final AttendanceSeedService seedSvc;

    public TeacherScheduleController(CourseRepository courseRepo,
                                     AttendanceSeedService seedSvc) {
        this.courseRepo = courseRepo;
        this.seedSvc = seedSvc;
    }

    /* ───────────────── helpers ───────────────── */

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

    /** 레포 수정 없이 classId/id/name 까지 유연 매칭 */
    private Course findCourseByClassIdFlexible(String anyIdOrName) {
        if (anyIdOrName == null) return null;
        String key = anyIdOrName.trim()
                .replaceAll("^\"|\"$", "")
                .replaceAll("\\s+", " ");

        List<Course> all = courseRepo.findAll();
        if (all.isEmpty()) return null;

        for (Course c : all) if (key.equals(c.getClassId())) return c;
        for (Course c : all) if (c.getClassId() != null && key.equalsIgnoreCase(c.getClassId())) return c;
        for (Course c : all) if (c.getId() != null && key.equals(c.getId())) return c;

        List<Course> nameMatches = new ArrayList<>();
        for (Course c : all) {
            if (c.getClassName() != null && key.equals(c.getClassName().trim())) nameMatches.add(c);
        }
        if (nameMatches.size() == 1) return nameMatches.get(0);

        String loose = key.replaceAll("[\\s:-]", "");
        for (Course c : all) {
            String cid = c.getClassId() == null ? "" : c.getClassId().replaceAll("[\\s:-]", "");
            if (loose.equalsIgnoreCase(cid)) return c;
        }
        return null;
    }

    public record ScheduleItem(
            String scheduleId,
            String teacherId,
            String date,
            String classId,
            String title,
            String startTime,
            String endTime,
            Integer roomNumber,
            String memo
    ) {}

    public record CreateScheduleReq(
            String date,        // YYYY-MM-DD
            String classId,
            String title,
            String startTime,   // HH:mm
            String endTime,     // HH:mm
            Integer roomNumber, // optional
            String memo
    ) {}

    /** 벌크 추가용 DTO */
    public record CreateSchedulesBulkReq(
            List<String> dates, // ["YYYY-MM-DD", ...]
            String classId,
            String startTime,   // optional 공통
            String endTime,     // optional 공통
            Integer roomNumber, // optional 공통
            String memo
    ) {}

    private static int toMonFirstDow(LocalDate d) { return d.getDayOfWeek().getValue(); }
    private static boolean isIn(String ymd, Collection<String> dates) {
        if (dates == null) return false;
        return dates.stream().filter(Objects::nonNull).anyMatch(ymd::equals);
    }
    private static String makeScheduleId(String classId, String ymd) { return classId + "_" + ymd; }
    private static String ns(String s) { return s == null ? "" : s; }

    private static boolean isHHmm(String t) { return t != null && t.matches("^\\d{2}:\\d{2}$"); }
    private static int toMin(String t) {
        if (!isHHmm(t)) return -1;
        String[] sp = t.split(":");
        return Integer.parseInt(sp[0]) * 60 + Integer.parseInt(sp[1]);
    }
    private static boolean overlaps(String s1, String e1, String s2, String e2) {
        int a1 = toMin(s1), a2 = toMin(e1), b1 = toMin(s2), b2 = toMin(e2);
        if (a1 < 0 || a2 < 0 || b1 < 0 || b2 < 0) return false;
        return a1 < b2 && b1 < a2; // [s,e)
    }

    // ⏰ 08:00 ~ 22:00 제한 (주간 캘린더와 맞춤)
    private static final int MIN_ALLOWED_MINUTES = 8 * 60;   // 08:00
    private static final int MAX_ALLOWED_MINUTES = 22 * 60;  // 22:00

    /**
     * 시작/종료 시간이
     * - HH:mm 형식인지
     * - end > start 인지
     * - 08:00 ~ 22:00 범위 안인지
     * 체크하고, 문제 없으면 null / 문제 있으면 한글 에러 메시지 반환
     */
    private static String validateTimeWindow(String start, String end) {
        if (!isHHmm(start) || !isHHmm(end)) {
            return "시간 형식이 올바르지 않습니다. HH:mm 형식이어야 합니다.";
        }
        int s = toMin(start);
        int e = toMin(end);
        if (e <= s) {
            return "종료 시간은 시작 시간보다 늦어야 합니다.";
        }
        if (s < MIN_ALLOWED_MINUTES) {
            return "너무 이른 시간입니다. 수업 시작 시간은 08:00 이후로만 등록할 수 있습니다.";
        }
        if (e > MAX_ALLOWED_MINUTES) {
            return "너무 늦은 시간입니다. 수업 종료 시간은 22:00 이전이어야 합니다.";
        }
        return null;
    }

    private static ScheduleItem toItem(Course c, String ymd) {
        Course.DailyTime dt = c.getTimeFor(ymd);
        Integer rn = c.getRoomFor(ymd);
        return new ScheduleItem(
                makeScheduleId(c.getClassId(), ymd),
                c.getTeacherId(),
                ymd,
                c.getClassId(),
                c.getClassName(),
                ns(dt.getStart()),
                ns(dt.getEnd()),
                rn,
                c.getSchedule()
        );
    }

    /* ── 조회: 특정 일 ── */
    @GetMapping("/{teacherId}/schedules/day")
    public List<ScheduleItem> getDay(@PathVariable String teacherId,
                                     @RequestParam String date,
                                     Authentication auth) {
        guardTeacherId(teacherId, auth);
        LocalDate target = LocalDate.parse(date.strip());
        int dow = toMonFirstDow(target);
        String ymd = target.toString();

        List<Course> myCourses = courseRepo.findByTeacherId(teacherId);
        List<ScheduleItem> out = new ArrayList<>();

        for (Course c : myCourses) {
            if (isIn(ymd, c.getCancelledDates())) continue;
            boolean weekly = Optional.ofNullable(c.getDaysOfWeek()).orElse(List.of()).stream()
                    .map(Object::toString).map(Integer::parseInt).anyMatch(n -> n == dow);
            boolean extra = isIn(ymd, c.getExtraDates());
            if (weekly || extra) out.add(toItem(c, ymd));
        }
        return out.stream()
                .sorted(Comparator.comparing(ScheduleItem::startTime).thenComparing(ScheduleItem::classId))
                .collect(Collectors.toList());
    }

    /* ── 조회: 범위 ── */
    @GetMapping(value = "/{teacherId}/schedules", params = {"from","to"})
    public List<ScheduleItem> list(@PathVariable String teacherId,
                                   @RequestParam String from,
                                   @RequestParam String to,
                                   Authentication auth) {
        guardTeacherId(teacherId, auth);
        LocalDate f = LocalDate.parse(from.strip());
        LocalDate t = LocalDate.parse(to.strip());

        List<Course> myCourses = courseRepo.findByTeacherId(teacherId);
        List<ScheduleItem> out = new ArrayList<>();

        for (LocalDate d = f; d.isBefore(t); d = d.plusDays(1)) {
            String ymd = d.toString();
            int dow = toMonFirstDow(d);
            for (Course c : myCourses) {
                if (isIn(ymd, c.getCancelledDates())) continue;
                boolean weekly = Optional.ofNullable(c.getDaysOfWeek()).orElse(List.of()).stream()
                        .map(Object::toString).map(Integer::parseInt).anyMatch(n -> n == dow);
                boolean extra = isIn(ymd, c.getExtraDates());
                if (weekly || extra) out.add(toItem(c, ymd));
            }
        }
        return out.stream()
                .sorted(Comparator.comparing(ScheduleItem::date)
                        .thenComparing(ScheduleItem::startTime)
                        .thenComparing(ScheduleItem::classId))
                .collect(Collectors.toList());
    }

    /* ── 생성: 날짜별 오버라이드 저장 + Attendance 즉시 생성 ── */
    @PostMapping("/{teacherId}/schedules")
    public ResponseEntity<?> create(@PathVariable String teacherId,
                                    @RequestBody CreateScheduleReq body,
                                    Authentication auth) {
        guardTeacherId(teacherId, auth);

        if (body == null || body.date() == null || body.date().isBlank())
            return ResponseEntity.badRequest().body("date 필드는 필수입니다. (YYYY-MM-DD)");
        if (body.classId() == null || body.classId().isBlank())
            return ResponseEntity.badRequest().body("classId 필드는 필수입니다.");

        Course c = findCourseByClassIdFlexible(body.classId());
        if (c == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("해당 반을 찾을 수 없습니다.");

        if (!(hasRole(auth, "DIRECTOR") || teacherId.equals(c.getTeacherId())))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("해당 반에 대한 권한이 없습니다.");

        LocalDate date = LocalDate.parse(body.date().strip());
        String ymd = date.toString();
        int dow = toMonFirstDow(date);

        String start = isHHmm(body.startTime()) ? body.startTime() : c.getTimeFor(ymd).getStart();
        String end   = isHHmm(body.endTime())   ? body.endTime()   : c.getTimeFor(ymd).getEnd();
        Integer room = (body.roomNumber() != null) ? body.roomNumber() : c.getRoomFor(ymd);

        // ⏰ 시간 형식 + 08:00~22:00 범위 체크 (한글 메시지)
        String timeErr = validateTimeWindow(start, end);
        if (timeErr != null) return ResponseEntity.badRequest().body(timeErr);
        if (room == null) return ResponseEntity.badRequest().body("roomNumber(강의실 번호)는 필수입니다.");

        // 같은 방(Room) 충돌 검사 — 모든 코스
        for (Course other : courseRepo.findAll()) {
            // ✅ 같은 반은 충돌 검사에서 제외
            if (Objects.equals(other.getClassId(), c.getClassId())) continue;

            if (!Objects.equals(room, other.getRoomFor(ymd))) continue;
            if (isIn(ymd, other.getCancelledDates())) continue;

            boolean weekly = Optional.ofNullable(other.getDaysOfWeek()).orElse(List.of()).stream()
                    .map(Object::toString).map(Integer::parseInt).anyMatch(n -> n == dow);
            boolean extra = isIn(ymd, other.getExtraDates());
            if (!(weekly || extra)) continue;

            Course.DailyTime odt = other.getTimeFor(ymd);
            String os = odt.getStart(), oe = odt.getEnd();
            if (overlaps(start, end, os, oe)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("같은 강의실에서 시간이 겹치는 다른 수업이 있습니다.");
            }
        }

        // 내 스케줄(방 무관) 시간 겹침 차단 — ✅ 같은 반은 제외
        List<ScheduleItem> myDay = getDay(teacherId, ymd, auth);
        boolean timeClash = myDay.stream()
                .filter(ev -> !Objects.equals(ev.classId(), c.getClassId()))
                .anyMatch(ev -> overlaps(start, end, ev.startTime(), ev.endTime()));
        if (timeClash) return ResponseEntity.status(HttpStatus.CONFLICT).body("선생님 본인 스케줄과 시간이 겹칩니다.");

        boolean matchesWeekly = Optional.ofNullable(c.getDaysOfWeek()).orElse(List.of()).stream()
                .map(Object::toString).map(Integer::parseInt).anyMatch(n -> n == dow);

        // 날짜별 오버라이드 저장
        c.putOverride(ymd, start, end, room);
        if (!matchesWeekly) {
            Set<String> extras = new LinkedHashSet<>(Optional.ofNullable(c.getExtraDates()).orElseGet(ArrayList::new));
            extras.add(ymd);
            c.setExtraDates(new ArrayList<>(extras));
        }
        courseRepo.save(c);

        // Attendance 즉시 생성
        seedSvc.ensureAttendanceForDate(c.getClassId(), ymd);

        return ResponseEntity.ok(toItem(c, ymd));
    }

    /* ── 벌크 생성: 여러 날짜 한꺼번에 + Attendance 일괄 생성 ── */
    @PostMapping("/{teacherId}/schedules/bulk")
    public ResponseEntity<?> createBulk(@PathVariable String teacherId,
                                        @RequestBody CreateSchedulesBulkReq body,
                                        Authentication auth) {
        guardTeacherId(teacherId, auth);

        if (body == null || body.classId() == null || body.classId().isBlank())
            return ResponseEntity.badRequest().body("classId 필드는 필수입니다.");
        if (body.dates() == null || body.dates().isEmpty())
            return ResponseEntity.badRequest().body("dates 목록은 비어 있을 수 없습니다.");

        Course c = findCourseByClassIdFlexible(body.classId());
        if (c == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("해당 반을 찾을 수 없습니다.");
        if (!(hasRole(auth, "DIRECTOR") || teacherId.equals(c.getTeacherId())))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("해당 반에 대한 권한이 없습니다.");

        List<ScheduleItem> created = new ArrayList<>();
        for (String raw : body.dates()) {
            if (raw == null || raw.isBlank()) continue;
            String ymd = LocalDate.parse(raw.strip()).toString();
            int dow = toMonFirstDow(LocalDate.parse(ymd));

            String start = isHHmm(body.startTime()) ? body.startTime() : c.getTimeFor(ymd).getStart();
            String end   = isHHmm(body.endTime())   ? body.endTime()   : c.getTimeFor(ymd).getEnd();
            Integer room = (body.roomNumber() != null) ? body.roomNumber() : c.getRoomFor(ymd);

            // ⏰ 벌크도 동일한 시간 제한 적용
            String timeErr = validateTimeWindow(start, end);
            if (timeErr != null) {
                return ResponseEntity.badRequest()
                        .body(ymd + " 날짜: " + timeErr);
            }
            if (room == null) return ResponseEntity.badRequest().body("roomNumber(강의실 번호)는 필수입니다.");

            // 간단 충돌 체크(동일 로직 재사용)
            for (Course other : courseRepo.findAll()) {
                // ✅ 같은 반은 충돌 검사에서 제외
                if (Objects.equals(other.getClassId(), c.getClassId())) continue;

                if (!Objects.equals(room, other.getRoomFor(ymd))) continue;
                if (isIn(ymd, other.getCancelledDates())) continue;

                boolean weekly = Optional.ofNullable(other.getDaysOfWeek()).orElse(List.of()).stream()
                        .map(Object::toString).map(Integer::parseInt).anyMatch(n -> n == dow);
                boolean extra = isIn(ymd, other.getExtraDates());
                if (!(weekly || extra)) continue;

                Course.DailyTime odt = other.getTimeFor(ymd);
                String os = odt.getStart(), oe = odt.getEnd();
                if (overlaps(start, end, os, oe)) {
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(ymd + " 날짜에 같은 강의실에서 시간이 겹치는 다른 수업이 있습니다.");
                }
            }

            // 저장
            c.putOverride(ymd, start, end, room);
            boolean matchesWeekly = Optional.ofNullable(c.getDaysOfWeek()).orElse(List.of()).stream()
                    .map(Object::toString).map(Integer::parseInt).anyMatch(n -> n == dow);
            if (!matchesWeekly) {
                Set<String> extras = new LinkedHashSet<>(Optional.ofNullable(c.getExtraDates()).orElseGet(ArrayList::new));
                extras.add(ymd);
                c.setExtraDates(new ArrayList<>(extras));
            }

            // 시드
            seedSvc.ensureAttendanceForDate(c.getClassId(), ymd);
            created.add(toItem(c, ymd));
        }

        courseRepo.save(c);
        created.sort(Comparator.comparing(ScheduleItem::date).thenComparing(ScheduleItem::startTime));
        return ResponseEntity.ok(created);
    }



    /* ── 오늘 수업 목록 ── */
    @GetMapping("/{teacherId}/classes/today")
    public List<Map<String,Object>> listToday(@PathVariable String teacherId,
                                              @RequestParam(required = false) String date,
                                              Authentication auth) {
        guardTeacherId(teacherId, auth);

        String ymd = (date == null || date.isBlank())
                ? LocalDate.now().toString()
                : LocalDate.parse(date.strip(), DateTimeFormatter.ofPattern("yyyy-MM-dd")).toString();

        int dow = LocalDate.parse(ymd).getDayOfWeek().getValue();
        List<Course> all = courseRepo.findByTeacherId(teacherId);
        if (all == null || all.isEmpty()) return List.of();

        List<Map<String,Object>> out = new ArrayList<>();
        for (Course c : all) {
            boolean weekly = Optional.ofNullable(c.getDaysOfWeek()).orElse(List.of())
                    .stream().map(Object::toString).map(Integer::parseInt).anyMatch(n -> n==dow);
            boolean extra = Optional.ofNullable(c.getExtraDates()).orElse(List.of()).contains(ymd);
            boolean cancel = Optional.ofNullable(c.getCancelledDates()).orElse(List.of()).contains(ymd);

            if ((weekly || extra) && !cancel) {
                Course.DailyTime dt = c.getTimeFor(ymd);
                Integer room = c.getRoomFor(ymd);
                Map<String,Object> m = new LinkedHashMap<>();
                m.put("classId", c.getClassId());
                m.put("title", c.getClassName());
                m.put("date", ymd);
                m.put("startTime", dt.getStart());
                m.put("endTime", dt.getEnd());
                m.put("roomNumber", room);
                out.add(m);
            }
        }
        out.sort(Comparator.comparing(m -> (String)m.get("startTime")));
        return out;
    }


}
