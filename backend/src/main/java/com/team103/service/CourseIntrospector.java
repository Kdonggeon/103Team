package com.team103.service;

import java.lang.reflect.Method;
import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Course 모델이 프로젝트마다 조금씩 다른 경우를 흡수하기 위한 안전한 리플렉션 어댑터.
 * 존재하면 쓰고, 없으면 건너뛰는 방식으로 최대한 값을 해석해준다.
 */
public final class CourseIntrospector {

    private CourseIntrospector() {}

    /* ===================== 기본 유틸 ===================== */

    private static Object call(Object target, String method, Class<?>[] types, Object... args) {
        try {
            Method m = target.getClass().getMethod(method, types);
            m.setAccessible(true);
            return m.invoke(target, args);
        } catch (Exception ignore) {
            return null;
        }
    }

    private static Object call(Object target, String method) {
        return call(target, method, new Class<?>[0]);
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object v, Class<T> type) {
        if (v == null) return null;
        if (type.isInstance(v)) return type.cast(v);
        return null;
    }

    private static Integer parseInt(Object v) {
        if (v == null) return null;
        try {
            if (v instanceof Number n) return n.intValue();
            return Integer.parseInt(String.valueOf(v).replaceAll("[^0-9-]", ""));
        } catch (Exception e) {
            return null;
        }
    }

    private static String asString(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v);
        return s != null && s.trim().isEmpty() ? null : s;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /* ===================== 공통 Getter ===================== */

    /** classId */
    public static String classId(Object c) {
        Object v = call(c, "getClassId");
        return asString(v);
    }

    /** className */
    public static String className(Object c) {
        Object v = call(c, "getClassName");
        return asString(v);
    }

    /** teacherId */
    public static String teacherId(Object c) {
        Object v = call(c, "getTeacherId");
        return asString(v);
    }

    /** academyNumber (단일) */
    public static Integer academyNumber(Object c) {
        Object v = call(c, "getAcademyNumber");
        return parseInt(v);
    }

    /** academyNumbers (복수) */
    @SuppressWarnings("unchecked")
    public static List<Integer> academyNumbers(Object c) {
        Object v = call(c, "getAcademyNumbers");
        if (v instanceof Collection<?> col) {
            List<Integer> out = new ArrayList<>();
            for (Object e : col) {
                Integer n = parseInt(e);
                if (n != null) out.add(n);
            }
            return out;
        }
        return Collections.emptyList();
    }

    /** roomFor: 날짜별 오버라이드가 있으면 우선 */
    public static Integer roomFor(Object c, String ymd) {
        // 1) getRoomFor(String)
        Object v1 = call(c, "getRoomFor", new Class<?>[]{String.class}, ymd);
        Integer n1 = parseInt(v1);
        if (n1 != null) return n1;

        // 2) getScheduleFor(String) → 객체에 getRoomNumber()
        Object sc = call(c, "getScheduleFor", new Class<?>[]{String.class}, ymd);
        Integer n2 = parseInt(call(sc, "getRoomNumber"));
        if (n2 != null) return n2;

        // 3) 기본 roomNumber
        Integer rn = parseInt(call(c, "getRoomNumber"));
        if (rn != null) return rn;

        // 4) roomNumbers 첫 번째
        @SuppressWarnings("unchecked")
        Collection<Object> rns = cast(call(c, "getRoomNumbers"), Collection.class);
        if (rns != null && !rns.isEmpty()) {
            for (Object o : rns) {
                Integer n = parseInt(o);
                if (n != null) return n;
            }
        }
        return null;
    }

    /** 오늘 수업 여부: 우선순위
     *  hasClassOn(yyyy-MM-dd) → hasClassOnDow(DayOfWeek) → roomFor(ymd)!=null → (start/end 존재 시 true)
     */
    public static boolean hasClassOn(Object c, String ymd, DayOfWeek dow) {
        Object a = call(c, "hasClassOn", new Class<?>[]{String.class}, ymd);
        if (a instanceof Boolean b) return b;

        Object b2 = call(c, "hasClassOnDow", new Class<?>[]{DayOfWeek.class}, dow);
        if (b2 instanceof Boolean bb) return bb;

        if (roomFor(c, ymd) != null) return true;

        String s = startTimeFor(c, ymd, dow);
        String e = endTimeFor(c, ymd, dow);
        return !isBlank(s) || !isBlank(e);
    }

    /** 시작/종료 시간 해석 */
    public static String startTimeFor(Object c, String ymd, DayOfWeek dow) {
        // 1) scheduleFor(ymd).getStartTime()
        Object sc = call(c, "getScheduleFor", new Class<?>[]{String.class}, ymd);
        String s1 = asString(call(sc, "getStartTime"));
        if (!isBlank(s1)) return s1;

        // 2) weeklySchedule.getStartTime(dow)
        Object ws = call(c, "getWeeklySchedule");
        String s2 = asString(call(ws, "getStartTime", new Class<?>[]{DayOfWeek.class}, dow));
        if (!isBlank(s2)) return s2;

        // 3) 필드형 getStartTime()
        String s3 = asString(call(c, "getStartTime"));
        if (!isBlank(s3)) return s3;

        return null;
    }

    public static String endTimeFor(Object c, String ymd, DayOfWeek dow) {
        Object sc = call(c, "getScheduleFor", new Class<?>[]{String.class}, ymd);
        String e1 = asString(call(sc, "getEndTime"));
        if (!isBlank(e1)) return e1;

        Object ws = call(c, "getWeeklySchedule");
        String e2 = asString(call(ws, "getEndTime", new Class<?>[]{DayOfWeek.class}, dow));
        if (!isBlank(e2)) return e2;

        String e3 = asString(call(c, "getEndTime"));
        if (!isBlank(e3)) return e3;

        return null;
    }

    /** students 목록(있으면) */
    @SuppressWarnings("unchecked")
    public static List<String> students(Object c) {
        Object v = call(c, "getStudents");
        if (v instanceof Collection<?> col) {
            return col.stream().map(String::valueOf).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
