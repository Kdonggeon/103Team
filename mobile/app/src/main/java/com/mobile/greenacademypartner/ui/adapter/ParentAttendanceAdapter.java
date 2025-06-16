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

        // ✅ SharedPreferences에서 자녀 ID 불러오기
        android.content.SharedPreferences prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        String studentId = prefs.getString("username", "");  // 학부모 로그인 시 저장된 자녀 ID

        // ✅ 해당 자녀의 출석 상태 찾기
        String matchedStatus = "출석 정보 없음";
        if (attendance.getAttendanceList() != null) {
            for (com.mobile.greenacademypartner.model.AttendanceEntry entry : attendance.getAttendanceList()) {
                if (entry.getStudentId().equals(studentId)) {
                    matchedStatus = entry.getStatus();
                    break;
                }
            }
        }

        // ✅ 텍스트로 표시
        holder.status.setText("출석: " + matchedStatus);
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
