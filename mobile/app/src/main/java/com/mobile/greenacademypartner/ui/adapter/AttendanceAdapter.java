package com.mobile.greenacademypartner.ui.adapter;

import android.content.Context;
import android.util.Log;
import android.view.*;
import android.widget.*;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.model.Attendance;

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
        holder.classNameView.setText(item.getClassName());
        holder.dateView.setText(item.getDate());
        holder.statusView.setText(item.getStatus());

        Log.d("AttendanceAdapter", "수업명: " + item.getClassName());


        return convertView;
    }
}
