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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AnswerAdapter extends RecyclerView.Adapter<AnswerAdapter.ViewHolder> {
    private List<Qa.Answer> items = new ArrayList<>();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault());

    public void setItems(List<Qa.Answer> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_answer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Qa.Answer answer = items.get(position);
        String meta = answer.getAuthorRole() + " • " + dateFormat.format(answer.getCreatedAt());
        holder.tvMeta.setText(meta);
        holder.tvContent.setText(answer.getContent());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMeta, tvContent;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMeta = itemView.findViewById(R.id.tvAnswerMeta);
            tvContent = itemView.findViewById(R.id.tvAnswerContent);
        }
    }
}
