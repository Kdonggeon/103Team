package com.mobile.greenacademypartner.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.model.Qa;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class QaAdapter extends RecyclerView.Adapter<QaAdapter.QaViewHolder> {

    private List<Qa> items;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault());

    public interface OnItemClickListener {
        void onItemClick(Qa qa);
    }

    private OnItemClickListener listener;

    public void setItems(List<Qa> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public QaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_qa, parent, false);
        return new QaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QaViewHolder holder, int position) {
        Qa item = items.get(position);
        holder.tvTitle.setText(item.getTitle());

        // Null 체크 추가: 날짜가 null이면 빈 문자열 또는 다른 표시로 대체
        Date createdAt = item.getCreatedAt();
        String formattedDate = (createdAt != null) ? dateFormat.format(createdAt) : "날짜 없음";
        String role = (item.getAuthorRole() != null) ? item.getAuthorRole() : "알 수 없음";


        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return (items != null) ? items.size() : 0;
    }

    static class QaViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;

        public QaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvItemQaTitle);
        }
    }
}
