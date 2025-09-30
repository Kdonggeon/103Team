
package com.mobile.greenacademypartner.ui.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.mobile.greenacademypartner.R;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ThreadAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public enum Type { USER_FOLLOWUP, TEACHER_ANSWER }

    public static class Item {
        public final Type type;
        public final String author;
        public final String content;
        public final String createdAt;

        public Item(Type type, String author, String content, String createdAt) {
            this.type = type;
            this.author = author;
            this.content = content;
            this.createdAt = createdAt;
        }
    }

    private final List<Item> items = new ArrayList<>();

    public void submit(List<Item> list) {
        items.clear();
        if (list != null) items.addAll(list);
        Collections.sort(items, Comparator.comparingLong(i -> {
            Date d = parseTime(i.createdAt);
            return d != null ? d.getTime() : Long.MIN_VALUE;
        }));
        notifyDataSetChanged();
    }

    @Override public int getItemViewType(int position) {
        return (items.get(position).type == Type.TEACHER_ANSWER) ? 1 : 0;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = (viewType == 1) ? R.layout.item_thread_teacher : R.layout.item_thread_user;
        View v = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new MsgVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int position) {
        ((MsgVH) h).bind(items.get(position));
    }

    @Override public int getItemCount() { return items.size(); }

    private static Date parseTime(String s) {
        if (TextUtils.isEmpty(s)) return null;
        String[] fmts = {
                "yyyy-MM-dd'T'HH:mm:ss.SSSX",
                "yyyy-MM-dd'T'HH:mm:ssX",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd"
        };
        for (String f : fmts) {
            try { return new SimpleDateFormat(f, Locale.KOREA).parse(s); }
            catch (Exception ignore) {}
        }
        return null;
    }

    static class MsgVH extends RecyclerView.ViewHolder {
        TextView tvAuthor, tvContent, tvDate;

        MsgVH(@NonNull View itemView) {
            super(itemView);
            // 기존 Answer 화면에서 쓰던 ID 이름을 그대로 사용
            tvAuthor  = itemView.findViewById(R.id.tv_answer_author);
            tvContent = itemView.findViewById(R.id.tv_answer_content);
            tvDate    = itemView.findViewById(R.id.tv_answer_date);
        }

        void bind(Item it) {
            if (tvAuthor  != null) tvAuthor.setText(it.author == null ? "" : it.author);
            if (tvContent != null) tvContent.setText(it.content == null ? "" : it.content);
            if (tvDate != null) {
                String t = formatTime(it.createdAt);
                if (t == null || t.isEmpty()) {
                    tvDate.setText("");
                    tvDate.setVisibility(View.GONE);
                } else {
                    tvDate.setText(t);
                    tvDate.setVisibility(View.VISIBLE);
                }
            }
        }

        private static String formatTime(String createdAt) {
            Date d = ThreadAdapter.parseTime(createdAt);
            if (d == null) return "";
            java.util.Calendar now = java.util.Calendar.getInstance();
            java.util.Calendar msg = java.util.Calendar.getInstance();
            msg.setTime(d);
            boolean sameDay = now.get(java.util.Calendar.YEAR) == msg.get(java.util.Calendar.YEAR)
                    && now.get(java.util.Calendar.DAY_OF_YEAR) == msg.get(java.util.Calendar.DAY_OF_YEAR);
            return new SimpleDateFormat(sameDay ? "HH:mm" : "M/d HH:mm", Locale.KOREA).format(d);
        }
    }
}
