package com.mobile.greenacademypartner.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.model.attendance.AttendanceResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 출석 카드 어댑터
 * 표기: 학원명, 수업명, 날짜(yyyy-MM-dd), 출석상태(출석/지각/결석)
 * 정렬: 날짜 오름차순(과거 → 최근)
 *
 * 주의: 카드 레이아웃은 item_attendance.xml (id: text_academy_name, text_class_name, text_date, text_status)
 */
public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.ViewHolder> {

    private final Context context;
    private final List<AttendanceResponse> items = new ArrayList<>();

    public AttendanceAdapter(Context context, List<AttendanceResponse> list) {
        this.context = context;
        replace(list);
    }

    /** 리스트 교체 + 날짜 오름차순 정렬 */
    public void replace(List<AttendanceResponse> list) {
        items.clear();
        if (list != null) items.addAll(list);
        // "yyyy-MM-dd" 기준 오름차순(시간순)
        Collections.sort(items, Comparator.comparing(AttendanceResponse::getDate, String::compareTo));
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attendance, parent, false); // ★ 출석 카드 레이아웃 사용
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AttendanceResponse item = items.get(position);

        String academy = safe(item.getAcademyName());
        String className = safe(item.getClassName());
        String date = safe(item.getDate());
        String status = safe(item.getStatus());

        holder.tvAcademy.setText(academy);
        holder.tvClass.setText(className);
        holder.tvDate.setText(date);
        holder.tvStatus.setText(status.isEmpty() ? "출석 정보 없음" : status);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvAcademy;
        final TextView tvClass;
        final TextView tvDate;
        final TextView tvStatus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAcademy = itemView.findViewById(R.id.text_academy_name);
            tvClass   = itemView.findViewById(R.id.text_class_name);
            tvDate    = itemView.findViewById(R.id.text_date);
            tvStatus  = itemView.findViewById(R.id.text_status);
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
