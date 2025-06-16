package com.mobile.greenacademypartner.ui.adapter;

import android.content.Context;
import android.util.Log;
import android.view.*;
import android.widget.*;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.model.Attendance;
import com.mobile.greenacademypartner.model.AttendanceEntry;

import java.util.List;

public class AttendanceAdapter extends BaseAdapter {
    private final Context context;
    private final List<Attendance> attendanceList;

    public AttendanceAdapter(Context context, List<Attendance> attendanceList) {
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
        TextView classNameView;
        TextView dateView;
        TextView statusView;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_attendance, parent, false);
            holder = new ViewHolder();
            holder.classNameView = convertView.findViewById(R.id.text_class_name);
            holder.dateView = convertView.findViewById(R.id.text_date);
            holder.statusView = convertView.findViewById(R.id.text_status);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Attendance item = attendanceList.get(position);
        Log.d("Adapter", "item.classId: " + item.getClassId() + ", date: " + item.getDate());

        holder.classNameView.setText(item.getClassId() != null ? item.getClassId() : "알 수 없음");
        holder.dateView.setText(item.getDate() != null ? item.getDate() : "날짜 없음");

        StringBuilder statusBuilder = new StringBuilder();
        List<AttendanceEntry> entries = item.getAttendanceList();
        if (entries != null) {
            for (AttendanceEntry entry : entries) {
                statusBuilder.append(entry.getStudentId())
                        .append(" - ")
                        .append(entry.getStatus())
                        .append("\n");
            }
        } else {
            statusBuilder.append("출석 정보 없음");
        }
        holder.statusView.setText(statusBuilder.toString().trim());

        return convertView;
    }
}
