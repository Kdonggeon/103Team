package com.mobile.greenacademypartner.ui.qna;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.api.AnswerApi;
import com.mobile.greenacademypartner.api.RetrofitClient;
import com.mobile.greenacademypartner.model.Answer;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AnswerAdapter extends RecyclerView.Adapter<AnswerAdapter.ViewHolder> {

    private List<Answer> answers = new ArrayList<>();
    private Context context;

    private String questionId;
    private String currentUserRole;

    // 생성자
    public AnswerAdapter(Context context, String currentUserId, String questionId) {
        this.context = context;
        this.currentUserRole = currentUserId != null ? currentUserId.trim() : "";
        this.questionId = questionId;
    }

    //  리스트 갱신
    public void submitList(List<Answer> list) {
        this.answers = list;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_answer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        Answer answer = answers.get(position);
        holder.bind(answer);

        holder.itemView.setOnClickListener(v -> {
            if ("teacher".equalsIgnoreCase(currentUserRole)) {
                showEditDeletePopup(answer);
            }
            Log.d("AnswerAdapter", "currentUserRole: [" + currentUserRole + "]");
        });
    }

    @Override
    public int getItemCount() {
        return answers.size();
    }

    // ✅ 팝업 다이얼로그
    private void showEditDeletePopup(Answer answer) {
        new AlertDialog.Builder(context)
                .setTitle("답변 관리")
                .setItems(new String[]{"수정", "삭제"}, (dialog, which) -> {
                    if (which == 0) {
                        openEditAnswerActivity(answer);
                    } else {
                        deleteAnswer(answer);
                    }
                })
                .show();
    }

    // ✅ 수정 → EditAnswerActivity 이동
    private void openEditAnswerActivity(Answer answer) {
        Intent intent = new Intent(context, EditAnswerActivity.class);
        intent.putExtra("answerId", answer.getId());
        intent.putExtra("questionId", questionId);
        context.startActivity(intent);
    }

    // ✅ 삭제 → 서버 호출
    private void deleteAnswer(Answer answer) {
        AnswerApi api = RetrofitClient.getClient().create(AnswerApi.class);
        api.deleteAnswer(answer.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(context, "삭제 완료", Toast.LENGTH_SHORT).show();
                    refresh();
                } else {
                    Toast.makeText(context, "삭제 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(context, "네트워크 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ✅ 새로고침
    private void refresh() {
        if (context instanceof QuestionDetailActivity) {
            ((QuestionDetailActivity) context).loadAnswerList();
        }
    }

    // ✅ ViewHolder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvContent;
        private TextView tvAuthor;
        private TextView tvDate;

        public ViewHolder(View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tv_answer_content);
            tvAuthor = itemView.findViewById(R.id.tv_answer_author);
            tvDate = itemView.findViewById(R.id.tv_answer_date);
        }

        public void bind(Answer answer) {
            tvContent.setText(answer.getContent());
            tvAuthor.setText(answer.getAuthor());
            if (answer.getCreatedAt() != null && answer.getCreatedAt().contains("T")) {
                String date = answer.getCreatedAt().split("T")[0];
                tvDate.setText(date);
            } else {
                tvDate.setText(answer.getCreatedAt());
            }
        }
    }
}
