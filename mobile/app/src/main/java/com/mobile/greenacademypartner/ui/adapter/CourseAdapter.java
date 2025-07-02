package com.mobile.greenacademypartner.ui.adapter;

import android.content.Context;
import android.view.*;
import android.widget.*;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.model.classes.Course;

import java.util.List;

public class CourseAdapter extends BaseAdapter {
    private final Context context;
    private final List<Course> courses;

    public CourseAdapter(Context context, List<Course> courses) {
        this.context = context;
        this.courses = courses;
    }

    @Override
    public int getCount() {
        return courses.size();
    }

    @Override
    public Object getItem(int position) {
        return courses.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    static class ViewHolder {
        TextView classNameView, scheduleView;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_course, parent, false);
            holder = new ViewHolder();
            holder.classNameView = convertView.findViewById(R.id.text_class_name);
            holder.scheduleView = convertView.findViewById(R.id.text_schedule);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Course course = courses.get(position);
        holder.classNameView.setText(course.getClassName());
        holder.scheduleView.setText(course.getSchedule());

        return convertView;
    }
}
