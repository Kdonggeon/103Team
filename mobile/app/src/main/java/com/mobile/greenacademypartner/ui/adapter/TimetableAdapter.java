package com.mobile.greenacademypartner.ui.adapter;

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
    private String displayDateIso;
    private final TimeZone tz = TimeZone.getTimeZone("Asia/Seoul");
    private final Locale loc = Locale.KOREA;

    // ---------------------------------------------------------
    // 생성자
    // ---------------------------------------------------------
    public TimetableAdapter(List<Course> initial) {

        // 기본 날짜 → 오늘 날짜
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", loc);
        sdf.setTimeZone(tz);
        this.displayDateIso = sdf.format(new Date());

        if (initial != null) {
            items.addAll(initial);
            sortByTime(items);
        }
    }

    // ---------------------------------------------------------
    // 날짜 변경 (ParentChildrenListActivity 반영용)
    // ---------------------------------------------------------
    public void setDisplayDate(String dateIso) {
        if (dateIso != null && !dateIso.isEmpty()) {
            this.displayDateIso = dateIso;
            notifyDataSetChanged();
        }
    }

    // ---------------------------------------------------------
    // 리스트 교체
    // ---------------------------------------------------------
    public void submit(List<Course> data) {
        items.clear();
        if (data != null) {
            items.addAll(data);
            sortByTime(items);
        }
        notifyDataSetChanged();
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
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attendance, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Course c = items.get(position);

        // ★ 수업명
        h.className.setText(c.getClassName());

        // ★ 학원명 (Course 객체에서 직접 가져옴)
        h.academyName.setText(c.getAcademyName());

        // ★ 날짜 (선택된 날짜 or 기본 날짜)
        h.date.setText(displayDateIso);

        // ★ 상태
        String st = c.getTodayStatus();
        h.status.setText(st != null ? st : "예정");

        // ★ 시간
        String s = normalize(c.getStartTime());
        String e = normalize(c.getEndTime());

        if (!s.isEmpty() && !e.isEmpty()) {
            h.timeRange.setText(s + "~" + e);
            h.timeRange.setVisibility(View.VISIBLE);
        } else {
            h.timeRange.setVisibility(View.GONE);
        }
    }

    // 시간 포맷 정리
    private String normalize(String t) {
        if (t == null || t.trim().isEmpty()) return "";
        try {
            String[] p = t.split(":");
            return String.format("%02d:%02d",
                    Integer.parseInt(p[0]),
                    Integer.parseInt(p[1]));
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView className, academyName, date, status, timeRange;

        VH(@NonNull View v) {
            super(v);
            className = v.findViewById(R.id.text_class_name);
            academyName = v.findViewById(R.id.text_academy_name);
            date = v.findViewById(R.id.text_date);
            status = v.findViewById(R.id.text_status);
            timeRange = v.findViewById(R.id.text_time_range);
        }
    }
}
