package com.mobile.greenacademypartner.ui.qna;

import android.text.TextUtils;
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

import java.util.List;
import java.util.Objects;

public class QuestionsAdapter extends ListAdapter<Question, QuestionsAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Question item);
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
                    // 동일 항목 식별 기준 (프로젝트의 실제 식별자에 맞추어 사용)
                    return Objects.equals(oldItem.getId(), newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull Question oldItem, @NonNull Question newItem) {
                    // 제목(학원명), 미확인 수, 최근 답변자, 교사 목록이 바뀌면 갱신
                    return Objects.equals(oldItem.getAcademyName(), newItem.getAcademyName())
                            && oldItem.getUnreadCount() == newItem.getUnreadCount()
                            && Objects.equals(oldItem.getRecentResponderNames(), newItem.getRecentResponderNames())
                            && Objects.equals(oldItem.getTeacherNames(), newItem.getTeacherNames());
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_question, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvTitle;
        final TextView tvResponder;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_item_question_title);
            tvResponder = itemView.findViewById(R.id.tv_item_question_responder);
        }

        void bind(@NonNull Question q, OnItemClickListener listener) {
            // 제목: 학원명 + 미읽음이면 문구만
            String baseTitle = (q.getAcademyName()!=null && !q.getAcademyName().trim().isEmpty())
                    ? q.getAcademyName().trim()
                    : (q.getAcademyNumber() > 0 ? "학원 " + q.getAcademyNumber() : "학원");

            if (q.getUnreadCount() > 0) {
                tvTitle.setText(baseTitle + " · 새로운 답변이 달렸습니다"); // ← 이름 붙이지 않음
            } else {
                tvTitle.setText(baseTitle);
            }

            // 제목 아래(회색): 미읽음일 때만 이름 노출
            java.util.List<String> names = null;
            if (q.getUnreadCount() > 0) {
                names = (q.getTeacherNames()!=null && !q.getTeacherNames().isEmpty())
                        ? q.getTeacherNames()
                        : q.getRecentResponderNames(); // 비어오면 recent로 대체
            }
            tvResponder.setText(
                    (names != null && !names.isEmpty())
                            ? "답변자: " + android.text.TextUtils.join(", ", names)
                            : "답변자: -"
            );

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(q);
            });
        }
    }
}
