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
    private String displayDateIso;
    private final TimeZone tz = TimeZone.getTimeZone("Asia/Seoul");
    private final Locale loc = Locale.KOREA;
    private final String prefAcademyName;

    public TimetableAdapter(Context ctx, List<Course> initial) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", loc);
        sdf.setTimeZone(tz);
        this.displayDateIso = sdf.format(new Date());

        SharedPreferences prefs = ctx.getSharedPreferences("login_prefs", Context.MODE_PRIVATE);
        this.prefAcademyName = prefs.getString("academyName", "");

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
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attendance, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Course c = items.get(position);

        // 수업명
        h.className.setText(c.getClassName() != null ? c.getClassName() : "");

        // 학원명
        String name = prefAcademyName != null ? prefAcademyName : "";
        if (name.isEmpty()) {
            SharedPreferences sp = h.itemView.getContext()
                    .getSharedPreferences("login_prefs", Context.MODE_PRIVATE);

            String nums = sp.getString("academyNumbers", "");
            if (nums != null && !nums.isEmpty()) {
                String[] parts = nums.replaceAll("[\\[\\]\"]", "").split(",");
                if (parts.length > 0) {
                    String firstRaw = parts[0].trim();
                    if (!firstRaw.isEmpty()) {
                        if (firstRaw.matches("\\d+")) name = firstRaw + "학원";
                        else name = firstRaw;
                    }
                }
            }
        }
        h.academyName.setText(name);

        // 날짜
        h.date.setText(displayDateIso);

        // 🔥 핵심: Activity에서 계산해준 todayStatus 표시
        String st = c.getTodayStatus();
        if (st != null && !st.isEmpty())
            h.status.setText(st);
        else
            h.status.setText("예정");

        // 시간
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

    private String normalizeTime(String t) {
        if (t == null || t.trim().isEmpty()) return "";
        try {
            String[] p = t.split(":");
            int hh = Integer.parseInt(p[0].trim());
            int mm = (p.length > 1) ? Integer.parseInt(p[1].trim()) : 0;
            return String.format(Locale.KOREA, "%02d:%02d", hh, mm);
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView className;
        final TextView academyName;
        final TextView date;
        final TextView status;
        final TextView timeRange;

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
