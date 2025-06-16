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

public class ParentAttendanceAdapter extends RecyclerView.Adapter<ParentAttendanceAdapter.ViewHolder> {

    private final Context context;
    private final List<Attendance> attendanceList;

    public ParentAttendanceAdapter(Context context, List<Attendance> attendanceList) {
        this.context = context;
        this.attendanceList = attendanceList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_parent_attendance, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Attendance attendance = attendanceList.get(position);
        holder.className.setText("수업: " + attendance.getClassName());
        holder.date.setText("날짜: " + attendance.getDate());
        holder.status.setText("출석: " + attendance.getStatus());
    }

    @Override
    public int getItemCount() {
        return attendanceList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView className, date, status;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            className = itemView.findViewById(R.id.text_class_name);
            date = itemView.findViewById(R.id.text_date);
            status = itemView.findViewById(R.id.text_status);
        }
    }
}
