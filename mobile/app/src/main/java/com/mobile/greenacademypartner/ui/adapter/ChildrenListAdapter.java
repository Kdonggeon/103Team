package com.mobile.greenacademypartner.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.view.LayoutInflater;
import android.widget.TextView;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.model.student.Student;
import com.mobile.greenacademypartner.ui.attendance.ChildAttendanceActivity;

import java.util.List;

public class ChildrenListAdapter extends BaseAdapter {

    private Context context;
    private List<Student> children;

    public ChildrenListAdapter(Context context, List<Student> children) {
        this.context = context;
        this.children = children;
    }

    @Override
    public int getCount() {
        return children.size();
    }

    @Override
    public Object getItem(int i) {
        return children.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }


    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.list_item_child, viewGroup, false);
        }

        TextView childName = view.findViewById(R.id.text_child_name);
        TextView childId = view.findViewById(R.id.text_child_id); // ✅ ID 텍스트도 동적으로

        Student student = children.get(position);

        //  이름과 ID를 실제 데이터로 표시
        childName.setText("이름: " + student.getStudentName());
        childId.setText("ID: " + student.getStudentId());

        view.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChildAttendanceActivity.class);
            intent.putExtra("studentId", student.getStudentId());
            context.startActivity(intent);
        });

        return view;
    }

}
