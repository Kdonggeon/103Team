package com.mobile.greenacademypartner.ui;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.model.Notice;
import java.util.List;

public class NoticeListAdapter extends RecyclerView.Adapter<NoticeListAdapter.Holder> {
    public interface OnClick {
        void onItemClick(Notice notice);
    }

    private final List<Notice> items;
    private final OnClick listener;

    public NoticeListAdapter(List<Notice> items, OnClick listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notice_title, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Notice notice = items.get(position);
        holder.tv.setText(notice.getTitle());

        // ✅ 공지 클릭 시 noticeId 전달
        holder.itemView.setOnClickListener(view -> {
            Intent intent = new Intent(view.getContext(), NoticeDetailActivity.class);
            intent.putExtra("NOTICE_ID", notice.getId());  // 🔥 반드시 추가
            view.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView tv;
        Holder(@NonNull View v) {
            super(v);
            tv = v.findViewById(R.id.tv_notice_title);
        }
    }
}
