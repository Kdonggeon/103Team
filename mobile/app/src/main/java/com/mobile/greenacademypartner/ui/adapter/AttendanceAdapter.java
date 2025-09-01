package com.mobile.greenacademypartner.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.model.attendance.Attendance;

import java.util.List;

public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.ViewHolder> {

    private final Context context;
    private final List<Attendance> attendanceList;

    public AttendanceAdapter(Context context, List<Attendance> attendanceList) {
        this.context = context;
        this.attendanceList = attendanceList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_timetable, parent, false); // per-item CardView 레이아웃
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Attendance item = attendanceList.get(position);

        String subject = item.getClassName();
        String date = item.getDate();

        // 상태 텍스트 (없으면 "출석 정보 없음")
        String statusText;
        if (item.getAttendanceList() != null && !item.getAttendanceList().isEmpty()) {
            String status = item.getAttendanceList().get(0).getStatus();
            statusText = (status != null && !status.trim().isEmpty()) ? status : "출석 정보 없음";
        } else {
            statusText = "출석 정보 없음";
        }

        StringBuilder detail = new StringBuilder();
        if (date != null && !date.trim().isEmpty()) {
            detail.append(date).append(" · ");
        }
        detail.append(statusText);

        holder.tvSubject.setText(subject != null ? subject : "");
        holder.tvDetail.setText(detail.toString());
    }

    @Override
    public int getItemCount() {
        return attendanceList == null ? 0 : attendanceList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvSubject;
        final TextView tvDetail;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSubject = itemView.findViewById(R.id.tv_item_timetable_subject);
            tvDetail  = itemView.findViewById(R.id.tv_item_timetable_detail);
        }
    }
}
