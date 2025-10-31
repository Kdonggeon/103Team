// src/main/java/com/team103/controller/TeacherScheduleController.java
package com.team103.controller;

import com.team103.model.Course;
import com.team103.repository.CourseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/manage/teachers")
public class TeacherScheduleController {

    private static final Logger log = LoggerFactory.getLogger(TeacherScheduleController.class);
    private final CourseRepository courseRepo;

    public TeacherScheduleController(CourseRepository courseRepo) {
        this.courseRepo = courseRepo;
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

    /** ✅ 레포 수정 없이 classId/id/name 까지 유연 매칭 */
    private Course findCourseByClassIdFlexible(String anyIdOrName) {
        if (anyIdOrName == null) return null;
        // 입력 정규화
        String key = anyIdOrName.trim()
                .replaceAll("^\"|\"$", "")     // 양끝 따옴표 제거
                .replaceAll("\\s+", " ");      // 중간 다중공백 정리

        List<Course> all = courseRepo.findAll();
        if (all.isEmpty()) return null;

        // 1) exact: classId
        for (Course c : all) {
            if (key.equals(c.getClassId())) return c;
        }
        // 2) case-insensitive: classId
        for (Course c : all) {
            if (c.getClassId() != null && key.equalsIgnoreCase(c.getClassId())) return c;
        }
        // 3) Mongo _id(문서 id)로 들어오는 경우
        for (Course c : all) {
            if (c.getId() != null && key.equals(c.getId())) return c;
        }
        // 4) className 으로 들어온 경우(이름이 유니크일 때만)
        List<Course> nameMatches = new ArrayList<>();
        for (Course c : all) {
            if (c.getClassName() != null && key.equals(c.getClassName().trim())) nameMatches.add(c);
        }
        if (nameMatches.size() == 1) return nameMatches.get(0);

        // 5) 마지막 시도: 공백/하이픈/콜론 제거 후 classId 비교
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

    /* ── 생성: 날짜별 오버라이드 저장 ── */

    @PostMapping("/{teacherId}/schedules")
    public ResponseEntity<?> create(@PathVariable String teacherId,
                                    @RequestBody CreateScheduleReq body,
                                    Authentication auth) {
        guardTeacherId(teacherId, auth);

        if (body == null || body.date() == null || body.date().isBlank())
            return ResponseEntity.badRequest().body("date required (YYYY-MM-DD)");
        if (body.classId() == null || body.classId().isBlank())
            return ResponseEntity.badRequest().body("classId required");

        // ✅ 레포 메서드 없이 classId로 찾기
        Course c = findCourseByClassIdFlexible(body.classId());
        if (c == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("class not found");

        if (!(hasRole(auth, "DIRECTOR") || teacherId.equals(c.getTeacherId())))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("not your class");

        LocalDate date = LocalDate.parse(body.date().strip());
        String ymd = date.toString();
        int dow = toMonFirstDow(date);

        String start = isHHmm(body.startTime()) ? body.startTime() : c.getTimeFor(ymd).getStart();
        String end   = isHHmm(body.endTime())   ? body.endTime()   : c.getTimeFor(ymd).getEnd();
        Integer room = (body.roomNumber() != null) ? body.roomNumber() : c.getRoomFor(ymd);

        if (!isHHmm(start) || !isHHmm(end)) return ResponseEntity.badRequest().body("invalid time (HH:mm required)");
        if (room == null) return ResponseEntity.badRequest().body("room required");

        // 같은 방(Room) 충돌 검사 — 모든 코스
        for (Course other : courseRepo.findAll()) {
            if (!Objects.equals(room, other.getRoomFor(ymd))) continue;
            if (isIn(ymd, other.getCancelledDates())) continue;

            boolean weekly = Optional.ofNullable(other.getDaysOfWeek()).orElse(List.of()).stream()
                    .map(Object::toString).map(Integer::parseInt).anyMatch(n -> n == dow);
            boolean extra = isIn(ymd, other.getExtraDates());
            if (!(weekly || extra)) continue;

            Course.DailyTime odt = other.getTimeFor(ymd);
            String os = odt.getStart(), oe = odt.getEnd();
            if (overlaps(start, end, os, oe)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("room conflict");
            }
        }

        // 내 스케줄(방 무관) 시간 겹침 차단
        List<ScheduleItem> myDay = getDay(teacherId, ymd, auth);
        boolean timeClash = myDay.stream().anyMatch(ev -> overlaps(start, end, ev.startTime(), ev.endTime()));
        if (timeClash) return ResponseEntity.status(HttpStatus.CONFLICT).body("time conflict");

        boolean matchesWeekly = Optional.ofNullable(c.getDaysOfWeek()).orElse(List.of()).stream()
                .map(Object::toString).map(Integer::parseInt).anyMatch(n -> n == dow);

        // ✅ 날짜별 오버라이드(시간/방) 저장
        c.putOverride(ymd, start, end, room);

        if (!matchesWeekly) {
            Set<String> extras = new LinkedHashSet<>(Optional.ofNullable(c.getExtraDates()).orElseGet(ArrayList::new));
            extras.add(ymd);
            c.setExtraDates(new ArrayList<>(extras));
        }

        courseRepo.save(c);
        return ResponseEntity.ok(toItem(c, ymd));
    }

    /* ── 삭제 ── */

    @DeleteMapping("/{teacherId}/schedules/{scheduleId}")
    public ResponseEntity<?> delete(@PathVariable String teacherId,
                                    @PathVariable String scheduleId,
                                    Authentication auth) {
        guardTeacherId(teacherId, auth);
        int idx = scheduleId.lastIndexOf('_');
        if (idx <= 0) return ResponseEntity.badRequest().body("invalid scheduleId");
        String classId = scheduleId.substring(0, idx);
        String ymd = scheduleId.substring(idx + 1);

        LocalDate date = LocalDate.parse(ymd);
        int dow = toMonFirstDow(date);

        // ✅ 레포 메서드 없이 classId로 찾기
        Course c = findCourseByClassIdFlexible(classId);
        if (c == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("class not found");
        if (!(hasRole(auth, "DIRECTOR") || teacherId.equals(c.getTeacherId())))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("not your class");

        // 오버라이드 제거
        c.clearOverride(ymd);

        // extras에서 제거되면 끝, 아니면 weekly면 cancelledDates에 추가
        List<String> extras = Optional.ofNullable(c.getExtraDates()).orElseGet(ArrayList::new);
        if (extras.removeIf(ymd::equals)) {
            c.setExtraDates(extras);
            courseRepo.save(c);
            return ResponseEntity.noContent().build();
        }

        boolean weekly = Optional.ofNullable(c.getDaysOfWeek()).orElse(List.of()).stream()
                .map(Object::toString).map(Integer::parseInt).anyMatch(n -> n == dow);
        if (weekly) {
            Set<String> cancels = new LinkedHashSet<>(Optional.ofNullable(c.getCancelledDates()).orElseGet(ArrayList::new));
            cancels.add(ymd);
            c.setCancelledDates(new ArrayList<>(cancels));
            courseRepo.save(c);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("schedule not found");
    }
}
