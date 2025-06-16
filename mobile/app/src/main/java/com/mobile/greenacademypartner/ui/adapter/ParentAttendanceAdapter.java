package com.mobile.greenacademypartner.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.model.Attendance;
import com.mobile.greenacademypartner.model.AttendanceEntry;

import java.util.List;

public class ParentAttendanceAdapter extends BaseAdapter {

    private final Context context;
    private final List<Attendance> attendanceList;

    public ParentAttendanceAdapter(Context context, List<Attendance> attendanceList) {
        this.context = context;
        this.attendanceList = attendanceList;
    }

    @Override
    public int getCount() {
        return attendanceList.size();
    }

    @Override
    public Object getItem(int position) {
        return attendanceList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    static class ViewHolder {
        TextView className, date, status;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        Attendance attendance = attendanceList.get(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_parent_attendance, parent, false);
            holder = new ViewHolder();
            holder.className = convertView.findViewById(R.id.text_class_name);
            holder.date = convertView.findViewById(R.id.text_date);
            holder.status = convertView.findViewById(R.id.text_status);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.className.setText("수업: " + attendance.getClassName());
        holder.date.setText("날짜: " + attendance.getDate());

        // ✅ 여러 학생 출석 상태 출력
        StringBuilder statusBuilder = new StringBuilder();
        List<AttendanceEntry> entries = attendance.getAttendanceList();
        if (entries != null) {
            for (AttendanceEntry entry : entries) {
                statusBuilder.append(entry.getStudentId())
                        .append(" - ")
                        .append(entry.getStatus())
                        .append("\n");
            }
        }

        holder.status.setText("출석:\n" + statusBuilder.toString().trim());

        return convertView;
    }
}
