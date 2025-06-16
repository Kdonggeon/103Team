package com.mobile.greenacademypartner.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.model.Attendance;

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
        View view = LayoutInflater.from(context).inflate(R.layout.item_attendance, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Attendance item = attendanceList.get(position);
        holder.textClassName.setText(item.getClassName());
        holder.textDate.setText(item.getDate());

        if (item.getAttendanceList() != null && !item.getAttendanceList().isEmpty()) {
            String status = item.getAttendanceList().get(0).getStatus();  // ✅ 첫 번째 출석 상태
            holder.textStatus.setText("1명 상태: " + status);
        } else {
            holder.textStatus.setText("출석 정보 없음");
        }
    }


    @Override
    public int getItemCount() {
        return attendanceList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textClassName, textDate, textStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textClassName = itemView.findViewById(R.id.text_class_name);
            textDate = itemView.findViewById(R.id.text_date);
            textStatus = itemView.findViewById(R.id.text_status);
        }
    }
}
