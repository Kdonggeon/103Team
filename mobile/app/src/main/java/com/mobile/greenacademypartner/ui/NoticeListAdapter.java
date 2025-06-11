package com.mobile.greenacademypartner.ui.adapter;

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
    public interface OnClick { void onItemClick(Notice notice); }
    private final List<Notice> items;
    private final OnClick listener;

    public NoticeListAdapter(List<Notice> items, OnClick listener) {
        this.items = items; this.listener = listener;
    }

    @NonNull @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup p, int i) {
        View v = LayoutInflater.from(p.getContext())
                .inflate(R.layout.item_notice_title, p, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int pos) {
        Notice n = items.get(pos);
        h.tv.setText(n.getTitle());
        h.itemView.setOnClickListener(view -> listener.onItemClick(n));
    }

    @Override public int getItemCount() { return items.size(); }

    static class Holder extends RecyclerView.ViewHolder {
        TextView tv;
        Holder(@NonNull View v) {
            super(v);
            tv = v.findViewById(R.id.tv_notice_title);
        }
    }
}
