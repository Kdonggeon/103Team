package com.mobile.greenacademypartner.ui.notification;

import com.google.gson.annotations.SerializedName;

public class NotificationItem {
    public enum Type { DOC, LINK, ATTACH }

    @SerializedName("type")   private String type;   // "DOC"/"LINK"/"ATTACH"
    @SerializedName("title")  private String title;
    @SerializedName("body")   private String subtitle;
    @SerializedName("time")   private String time;   // e.g. "1일 전"
    @SerializedName("icon")   private String icon;   // 서버가 줄 수도 있음(옵션)

    // 기존 생성자/Getter 유지
    public NotificationItem(Type t, String title, String subtitle, String time) {
        this.type = t.name(); this.title = title; this.subtitle = subtitle; this.time = time;
    }
    public NotificationItem() {}

    public Type getType() {
        try { return Type.valueOf(type); } catch (Exception e) { return Type.DOC; }
    }
    public String getTitle()    { return title; }
    public String getSubtitle() { return subtitle; }
    public String getTime()     { return time; }
}
