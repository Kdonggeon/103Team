package com.mobile.greenacademypartner.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.model.teacher.TeacherAttendance;

import java.util.List;

public class TeacherAttendanceAdapter extends RecyclerView.Adapter<TeacherAttendanceAdapter.ViewHolder> {

    private final List<TeacherAttendance> attendanceList;

    public TeacherAttendanceAdapter(List<TeacherAttendance> attendanceList) {
        this.attendanceList = attendanceList;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textDate, textClassId, textAttendanceList;

        public ViewHolder(View view) {
            super(view);
            textDate = view.findViewById(R.id.text_date);
            textClassId = view.findViewById(R.id.text_class_id);
            textAttendanceList = view.findViewById(R.id.text_attendance_list);
        }
    }

    @NonNull
    @Override
    public TeacherAttendanceAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_teacher_attendance, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TeacherAttendanceAdapter.ViewHolder holder, int position) {
        TeacherAttendance item = attendanceList.get(position);
        holder.textDate.setText("날짜: " + item.getDate());
        holder.textClassId.setText("수업 ID: " + item.getClassId());

        StringBuilder sb = new StringBuilder();

        // ✅ Null 체크 추가
        List<TeacherAttendance.AttendanceRecord> records = item.getAttendanceList();
        if (records != null) {
            for (TeacherAttendance.AttendanceRecord record : records) {
                sb.append(record.getStudentId() != null ? record.getStudentId() : "알 수 없음")
                        .append(" - ")
                        .append(record.getStatus() != null ? record.getStatus() : "미정")
                        .append("\n");
            }
        } else {
            sb.append("출석 정보 없음");
        }

        holder.textAttendanceList.setText(sb.toString().trim());
    }



    @Override
    public int getItemCount() {
        return attendanceList.size();
    }
}
