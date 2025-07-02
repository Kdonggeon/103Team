package com.mobile.greenacademypartner.ui.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.model.teacher.TeacherClass;

import java.util.List;

public class TeacherClassAdapter extends RecyclerView.Adapter<TeacherClassAdapter.ViewHolder> {

    public interface OnClassClickListener {
        void onClick(String classId);
    }

    private final List<TeacherClass> classList;
    private final OnClassClickListener listener;

    public TeacherClassAdapter(List<TeacherClass> classList, OnClassClickListener listener) {
        this.classList = classList;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView className, schedule;

        public ViewHolder(View view) {
            super(view);
            className = view.findViewById(R.id.text_class_name);
            schedule = view.findViewById(R.id.text_schedule);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_teacher_class, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        TeacherClass course = classList.get(position);
        Log.d("어댑터", "바인딩 위치: " + position + ", 수업 이름: " + course.getClassName());
        holder.className.setText(course.getClassName());
        holder.schedule.setText(course.getSchedule());

        holder.itemView.setOnClickListener(v -> {
            listener.onClick(course.getClassId());
        });
    }

    @Override
    public int getItemCount() {
        return classList.size(); // ✅ 반드시 > 0
    }
}
