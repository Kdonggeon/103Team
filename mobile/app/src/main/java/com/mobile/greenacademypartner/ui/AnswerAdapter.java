package com.mobile.greenacademypartner.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.model.Answer;

import java.util.ArrayList;
import java.util.List;

public class AnswerAdapter extends RecyclerView.Adapter<AnswerAdapter.AnswerViewHolder> {

    private final List<Answer> answers = new ArrayList<>();

    public void submitList(List<Answer> newList) {
        answers.clear();
        if (newList != null) {
            answers.addAll(newList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AnswerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_answer, parent, false);
        return new AnswerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AnswerViewHolder holder, int position) {
        Answer answer = answers.get(position);
        holder.bind(answer);
    }

    @Override
    public int getItemCount() {
        return answers.size();
    }

    static class AnswerViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvContent;
        private final TextView tvAuthor;
        private final TextView tvDate;

        public AnswerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tv_answer_content);
            tvAuthor = itemView.findViewById(R.id.tv_answer_author);
            tvDate = itemView.findViewById(R.id.tv_answer_date);
        }

        public void bind(Answer answer) {
            if (answer != null) {
                tvContent.setText(answer.getContent() != null ? answer.getContent() : "");
                tvAuthor.setText(answer.getAuthor() != null ? answer.getAuthor() : "");

                String dateText = answer.getCreatedAt();
                if (dateText != null && dateText.contains("T")) {
                    String onlyDate = dateText.split("T")[0];
                    tvDate.setText(onlyDate);
                } else {
                    tvDate.setText(dateText != null ? dateText : "");
                }
            }
        }
    }
}
