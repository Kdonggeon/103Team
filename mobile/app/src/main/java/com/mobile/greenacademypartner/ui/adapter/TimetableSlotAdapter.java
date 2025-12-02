package com.mobile.greenacademypartner.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.model.timetable.SlotDto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Slot 기반 시간표 어댑터 */
public class TimetableSlotAdapter extends RecyclerView.Adapter<TimetableSlotAdapter.VH> {

    private final List<SlotDto> items = new ArrayList<>();
    private String displayDateIso = LocalDate.now().toString();

    public TimetableSlotAdapter(List<SlotDto> initial) {
        if (initial != null) {
            items.addAll(initial);
            sortByTime(items);
        }
    }

    public void setDisplayDate(String dateIso) {
        if (dateIso != null && !dateIso.isEmpty()) {
            this.displayDateIso = dateIso;
            notifyDataSetChanged();
        }
    }

    public void submit(List<SlotDto> data) {
        items.clear();
        if (data != null) {
            items.addAll(data);
            sortByTime(items);
        }
        notifyDataSetChanged();
    }

    private void sortByTime(List<SlotDto> list) {
        list.sort(Comparator.comparing(s -> s.startTime != null ? s.startTime : ""));
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attendance, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {

        SlotDto s = items.get(pos);

        // ★ 순수 수업명
        h.className.setText(s.className);

        // ★ 학원이름 (수업명 아래, 따로 표시)
        h.academyName.setText(
                s.academyName != null ? s.academyName : ""
        );

        // ★ 날짜
        h.date.setText(displayDateIso);

        // ★ 상태 계산
        h.status.setText(getStatus(s));

        // ★ 시간
        h.timeRange.setText(formatTime(s.startTime, s.endTime));
    }

    private String getStatus(SlotDto s) {

        LocalDate selected = LocalDate.parse(displayDateIso);
        LocalDate today = LocalDate.now();

        if (selected.isAfter(today)) return "예정";
        if (selected.isBefore(today)) return "종료";

        // 오늘이면 시간 비교
        if (s.startTime == null || s.endTime == null) return "수업";

        LocalTime now = LocalTime.now();
        LocalTime start = LocalTime.parse(s.startTime);
        LocalTime end = LocalTime.parse(s.endTime);

        if (now.isBefore(start)) return "예정";
        if (now.isAfter(end)) return "종료";
        return "진행중";
    }

    private String formatTime(String s, String e) {
        if (s == null || e == null) return "";
        return s + "~" + e;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {

        TextView className, academyName, date, status, timeRange;

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
