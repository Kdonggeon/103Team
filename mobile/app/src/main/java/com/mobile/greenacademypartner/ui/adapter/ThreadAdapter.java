package com.mobile.greenacademypartner.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mobile.greenacademypartner.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ThreadAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static class Item {
        public enum Type { TEACHER_ANSWER, USER_FOLLOWUP }
        public Type type;
        public String author;     // 표시 이름(있으면) → 없으면 ID
        public String content;
        public String createdAt;

        public Item(Type type, String author, String content, String createdAt) {
            this.type = type;
            this.author = author;
            this.content = content;
            this.createdAt = createdAt;
        }
    }

    private final List<Item> items = new ArrayList<>();

    public void submit(List<Item> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @Override public int getItemCount() { return items.size(); }

    @Override public int getItemViewType(int position) {
        return items.get(position).type == Item.Type.TEACHER_ANSWER ? 1 : 0;
    }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == 1) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_teacher, parent, false);
            return new TeacherVH(v);
        } else {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_student, parent, false);
            return new UserVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Item it = items.get(position);
        if (holder instanceof TeacherVH) {
            ((TeacherVH) holder).bind(it);
        } else if (holder instanceof UserVH) {
            ((UserVH) holder).bind(it);
        }
    }

    // 교사 메시지
    static class TeacherVH extends RecyclerView.ViewHolder {
        TextView tvContent, tvMeta;

        TeacherVH(@NonNull View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tv_message_content);
            tvMeta    = itemView.findViewById(R.id.tv_message_meta);
        }
        void bind(Item it) {
            String authorText = (it.author == null) ? "" : it.author;
            tvContent.setText(it.content == null ? "" : it.content);
            tvMeta.setText(authorText + " • " + formatMd(it.createdAt));
        }
    }

    // 학생/학부모 메시지
    static class UserVH extends RecyclerView.ViewHolder {
        TextView tvContent, tvMeta;

        UserVH(@NonNull View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tv_message_content);
            tvMeta    = itemView.findViewById(R.id.tv_message_meta);
        }
        void bind(Item it) {
            String authorText = (it.author == null) ? "" : it.author;
            tvContent.setText(it.content == null ? "" : it.content);
            tvMeta.setText(authorText + " • " + formatMd(it.createdAt));
        }
    }

    // "월/일"만 표기 (서버 포맷 다양성 대응)
    private static String formatMd(String createdAt) {
        if (createdAt == null) return "";
        for (String fmt : Arrays.asList(
                "yyyy-MM-dd'T'HH:mm:ss.SSSX",
                "yyyy-MM-dd'T'HH:mm:ssX",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd"
        )) {
            try {
                Date d = new SimpleDateFormat(fmt, Locale.KOREA).parse(createdAt);
                return new SimpleDateFormat("M월 d일", Locale.KOREA).format(d);
            } catch (ParseException ignored) {}
        }
        return createdAt;
    }

    public void append(Item item) {
        if (item == null) return;
        int pos = items.size();
        items.add(item);
        notifyItemInserted(pos);
    }
}
