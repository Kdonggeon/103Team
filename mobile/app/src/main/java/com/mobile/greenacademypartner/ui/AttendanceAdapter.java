package com.mobile.greenacademypartner.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.model.Attendance;

import java.util.List;

public class AttendanceAdapter extends ArrayAdapter<Attendance> {

    public AttendanceAdapter(Context context, List<Attendance> records) {
        super(context, 0, records);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Attendance record = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_attendance, parent, false);
        }

        TextView classIdView = convertView.findViewById(R.id.text_class_id);
        TextView dateView = convertView.findViewById(R.id.text_date);
        TextView statusView = convertView.findViewById(R.id.text_status);

        classIdView.setText("수업: " + record.getClassId());
        dateView.setText("날짜: " + record.getDate());
        statusView.setText("상태: " + record.getStatus());

        return convertView;
    }
}
