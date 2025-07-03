package com.mobile.greenacademypartner.ui.qna;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.model.Question;

public class QuestionsAdapter extends ListAdapter<Question, QuestionsAdapter.ViewHolder> {
    public interface OnItemClickListener {
        void onItemClick(Question question);
    }

    private final OnItemClickListener listener;

    public QuestionsAdapter(OnItemClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Question> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Question>() {
                @Override
                public boolean areItemsTheSame(@NonNull Question oldItem, @NonNull Question newItem) {
                    return oldItem.getId().equals(newItem.getId());
                }
                @Override
                public boolean areContentsTheSame(@NonNull Question oldItem, @NonNull Question newItem) {
                    return oldItem.getTitle().equals(newItem.getTitle())
                            && oldItem.getContent().equals(newItem.getContent());
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_question, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Question q = getItem(position);
        holder.bind(q, listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle;
        private final TextView tvContent;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_item_question_title);
            tvContent = itemView.findViewById(R.id.tv_item_question_content);
        }

        public void bind(Question question, OnItemClickListener listener) {
            tvTitle.setText(question.getTitle());
            tvContent.setText(question.getContent());
            itemView.setOnClickListener(v -> listener.onItemClick(question));
        }
    }
}
