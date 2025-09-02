package com.mobile.greenacademypartner.ui.qna;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.model.Question;

import java.util.List;
import java.util.Objects;

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
                    return Objects.equals(oldItem.getId(), newItem.getId());
                }
                @Override
                public boolean areContentsTheSame(@NonNull Question oldItem, @NonNull Question newItem) {
                    // ✅ 카드에 보이는 값: 제목 + 교사목록
                    return Objects.equals(oldItem.getTitle(), newItem.getTitle())
                            && Objects.equals(oldItem.getTeacherNames(), newItem.getTeacherNames());
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_question, parent, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle;
        private final TextView tvContent;

        ViewHolder(@NonNull android.view.View itemView) {
            super(itemView);
            tvTitle   = itemView.findViewById(R.id.tv_item_question_title);
            tvContent = itemView.findViewById(R.id.tv_item_question_content);
        }

        void bind(Question q, OnItemClickListener listener) {
            // ✅ 제목은 카드의 큰 글씨
            tvTitle.setText(q.getTitle() == null ? "" : q.getTitle());

            // ✅ 서브라인: 교사들 문구
            String sub;
            List<String> names = q.getTeacherNames();
            if (names != null && !names.isEmpty()) {
                if (names.size() == 1) {
                    String name = names.get(0);
                    sub = name + josaIGa(name) + " 답변";
                } else {
                    String joined = TextUtils.join(", ", names);
                    String last   = names.get(names.size() - 1);
                    sub = joined + josaIGa(last) + " 답변";
                }
            } else {
                sub = "미답변";
            }
            tvContent.setText(sub);

            itemView.setOnClickListener(v -> listener.onItemClick(q));
        }

        // 한국어 조사(이/가)
        private static String josaIGa(String word) {
            if (word == null || word.isEmpty()) return "이";
            char ch = word.charAt(word.length() - 1);
            if (ch >= 0xAC00 && ch <= 0xD7A3) {
                int base = ch - 0xAC00;
                boolean hasFinal = (base % 28) != 0;
                return hasFinal ? "이" : "가";
            }
            return "이";
        }
    }
}
