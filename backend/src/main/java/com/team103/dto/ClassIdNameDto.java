package com.team103.dto;

import com.team103.model.Course;

/** 과목 셀렉터용 초경량 DTO */
public class ClassIdNameDto {
    private String id;
    private String name;

    public ClassIdNameDto() {}
    public ClassIdNameDto(String id, String name) {
        this.id = id; this.name = name;
    }

    public static ClassIdNameDto fromCourse(Course c) {
        if (c == null) return new ClassIdNameDto(null, null);
        // id 후보: classId 우선, 없으면 Mongo _id
        String id = nz(c.getClassId(), c.getId());
        // name 후보: className 우선, 없으면 name/title 같은 건 없으니 비워둔다(※ 중요: id로 폴백하지 않음)
        String name = nz(c.getClassName(), null);
        return new ClassIdNameDto(id, name);
    }

    // --- utils ---
    private static boolean blank(String s){ return s == null || s.trim().isEmpty(); }
    /** 첫 번째 non-blank 반환 */
    private static String nz(String a, String b) {
        if (!blank(a)) return a;
        if (!blank(b)) return b;
        return null;
    }

    // --- getters/setters ---
    public String getId(){ return id; }
    public String getName(){ return name; }
    public void setId(String id){ this.id = id; }
    public void setName(String name){ this.name = name; }
}
