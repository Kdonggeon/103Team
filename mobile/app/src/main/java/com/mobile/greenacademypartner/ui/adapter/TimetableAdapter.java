package com.mobile.greenacademypartner.ui.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.model.classes.Course;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;


public class TimetableAdapter extends RecyclerView.Adapter<TimetableAdapter.VH> {

    private final List<Course> items = new ArrayList<>();
    private String displayDateIso;          // "yyyy-MM-dd" (표시용 날짜)
    private final TimeZone tz = TimeZone.getTimeZone("Asia/Seoul");
    private final Locale   loc = Locale.KOREA;
    private final String prefAcademyName;   // 저장된 학원명(없으면 빈 문자열)

    public TimetableAdapter(Context ctx, List<Course> initial) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", loc);
        sdf.setTimeZone(tz);
        this.displayDateIso = sdf.format(new Date()); // 기본: 오늘

        SharedPreferences prefs = ctx.getSharedPreferences("login_prefs", Context.MODE_PRIVATE);
        this.prefAcademyName = prefs.getString("academyName", ""); // 없으면 빈 문자열

        if (initial != null) {
            items.addAll(initial);
            sortByTime(items);
        }
    }

    public void submit(List<Course> data) {
        items.clear();
        if (data != null) {
            items.addAll(data);
            sortByTime(items);
        }
        notifyDataSetChanged();
    }

    // 선택 날짜(yyyy-MM-dd)를 어댑터에 반영
    public void setDisplayDate(String dateIso) {
        if (dateIso != null && !dateIso.isEmpty()) {
            this.displayDateIso = dateIso;
            notifyDataSetChanged();
        }
    }

    private void sortByTime(List<Course> list) {
        list.sort(Comparator.comparing(
                c -> c.getStartTime() != null ? c.getStartTime() : "",
                String::compareTo
        ));
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 기존 출석 이력 아이템 레이아웃 그대로 사용
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attendance, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Course c = items.get(position);

        // 1) 수업명
        h.className.setText(c.getClassName() != null ? c.getClassName() : "");

        // 2) 학원명(공백 허용: 임시 대체 로직은 사용자 프로젝트 상황에 맞게 유지/삭제)
        String name = (prefAcademyName != null) ? prefAcademyName : "";
        if (name == null || name.isEmpty()) {
            SharedPreferences sp = h.itemView.getContext()
                    .getSharedPreferences("login_prefs", android.content.Context.MODE_PRIVATE);

            // 예: "[103]" 또는 "[103학원]"
            String nums = sp.getString("academyNumbers", "");
            if (nums != null && !nums.isEmpty()) {
                String[] parts = nums.replaceAll("[\\[\\]\"]", "").split(",");
                if (parts.length > 0) {
                    String firstRaw = parts[0].trim();
                    if (!firstRaw.isEmpty()) {
                        if (firstRaw.matches("\\d+")) {
                            name = firstRaw + "학원";
                        } else {
                            name = firstRaw;
                        }
                    }
                }
            }
        }
        h.academyName.setText(name != null ? name : "");

        // 3) 날짜: 선택 날짜(기본은 오늘)
        h.date.setText(displayDateIso != null ? displayDateIso : "");

        // 4) 상태: 시간대 기반 간단 상태 (예정/진행중/종료)
        String statusText = "예정";
        int startMin = parseMinuteOfDay(c.getStartTime());
        int endMin   = parseMinuteOfDay(c.getEndTime());
        if (startMin >= 0 && endMin >= 0) {
            java.util.Calendar now = java.util.Calendar.getInstance(tz, loc);
            int nowMin = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE);
            if (nowMin < startMin)      statusText = "예정";
            else if (nowMin > endMin)   statusText = "종료";
            else                        statusText = "진행중";
        }
        h.status.setText(statusText);

        // 5) 시간 범위: "HH:mm~HH:mm"
        if (h.timeRange != null) {
            String s = normalizeTime(c.getStartTime());
            String e = normalizeTime(c.getEndTime());
            if (!s.isEmpty() && !e.isEmpty()) {
                h.timeRange.setText(s + "~" + e);
                h.timeRange.setVisibility(View.VISIBLE);
            } else {
                h.timeRange.setText("");
                h.timeRange.setVisibility(View.GONE);
            }
        }
    }

    private int parseMinuteOfDay(String hhmm) {
        if (hhmm == null) return -1;
        String[] sp = hhmm.split(":");
        if (sp.length != 2) return -1;
        try {
            int h = Integer.parseInt(sp[0].trim());
            int m = Integer.parseInt(sp[1].trim());
            if (h < 0 || h > 23 || m < 0 || m > 59) return -1;
            return h * 60 + m;
        } catch (Exception e) {
            return -1;
        }
    }

    // "H:mm" / "HH:mm" / "9:0" → "HH:mm"
    private String normalizeTime(String t) {
        if (t == null || t.trim().isEmpty()) return "";
        try {
            String[] p = t.split(":");
            int hh = Integer.parseInt(p[0].trim());
            int mm = (p.length > 1) ? Integer.parseInt(p[1].trim()) : 0;
            if (hh < 0 || hh > 23 || mm < 0 || mm > 59) return "";
            return String.format(Locale.KOREA, "%02d:%02d", hh, mm);
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView className;     // @id/text_class_name
        final TextView academyName;   // @id/text_academy_name
        final TextView date;          // @id/text_date
        final TextView status;        // @id/text_status
        final TextView timeRange;     // @id/text_time_range (레이아웃에 추가되어 있어야 함)
        VH(@NonNull View itemView) {
            super(itemView);
            className   = itemView.findViewById(R.id.text_class_name);
            academyName = itemView.findViewById(R.id.text_academy_name);
            date        = itemView.findViewById(R.id.text_date);
            status      = itemView.findViewById(R.id.text_status);
            timeRange   = itemView.findViewById(R.id.text_time_range);
        }
    }
}
