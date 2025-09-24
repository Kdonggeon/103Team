package com.mobile.greenacademypartner.ui.qna;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
    private final Context context;

    private final String questionId;
    private final String currentUserRole;

    // 생성자
    public AnswerAdapter(Context context, String questionId, String currentUserRole) {
        this.context = context;
        this.questionId = questionId;
        this.currentUserRole = currentUserRole != null ? currentUserRole.trim() : "";
    }

    // 리스트 갱신
    public void submitList(List<Answer> list) {
        this.answers = list != null ? list : new ArrayList<>();
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
            if (!"TEACHER".equalsIgnoreCase(currentUserRole)) {
                Log.d("AnswerAdapter", "currentUserRole: [" + currentUserRole + "]");
                return;
            }

            String clickedId = answer.getId();

            refresh(() -> {
                Answer refreshedAnswer = null;
                for (Answer a : answers) {
                    if (a.getId().equals(clickedId)) {
                        refreshedAnswer = a;
                        break;
                    }
                }

                if (refreshedAnswer != null) {
                    showEditDeletePopup(refreshedAnswer);
                } else {
                    Toast.makeText(context, "이 답변은 삭제되었거나 수정되었습니다.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public int getItemCount() {
        return answers.size();
    }

    // 팝업 다이얼로그
    private void showEditDeletePopup(Answer answer) {
        new AlertDialog.Builder(context)
                .setTitle("답변 관리")
                .setItems(new String[]{"수정", "삭제"}, (dialog, which) -> {
                    if (which == 0) {
                        Log.d("AnswerAdapter", "[Popup] Answer ID: " + answer.getId());
                        Log.d("AnswerAdapter", "[Popup] Question ID: " + questionId);
                        openEditAnswerActivity(answer);
                    } else {
                        Log.d("AnswerAdapter", "[Popup] Answer ID: " + answer.getId());
                        Log.d("AnswerAdapter", "[Popup] Question ID: " + questionId);
                        deleteAnswer(answer);
                    }
                })
                .show();
    }

    // 수정 → EditAnswerActivity 이동
    private void openEditAnswerActivity(Answer answer) {
        Intent intent = new Intent(context, EditAnswerActivity.class);
        intent.putExtra("answerId", answer.getId());
        intent.putExtra("questionId", questionId);
        context.startActivity(intent);
    }

    private void deleteAnswer(Answer answer) {
        Log.d("AnswerAdapter", "[Delete] Attempting to delete AnswerID: " + answer.getId());

        String auth = getAuthHeader(context);
        AnswerApi api = RetrofitClient.getClient().create(AnswerApi.class);
        api.deleteAnswer(auth, answer.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                Log.d("AnswerAdapter", "[Delete] Response code: " + response.code());
                if (response.isSuccessful()) {
                    Toast.makeText(context, "삭제 완료", Toast.LENGTH_SHORT).show();
                    refresh();
                } else if (response.code() == 403) {
                    Toast.makeText(context, "접근 권한이 없습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "삭제 실패(" + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("AnswerAdapter", "[Delete] Network Failure", t);
                Toast.makeText(context, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 기존 새로고침 (콜백 없는 기본)
    private void refresh() {
        refresh(() -> {});
    }

    // 콜백 받는 새로고침
    private void refresh(Runnable onComplete) {
        if (context instanceof QuestionDetailActivity) {
            ((QuestionDetailActivity) context).loadAnswerList(questionId, onComplete);
        } else {
            onComplete.run();
        }
    }

    // Authorization 헤더 생성
    private String getAuthHeader(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences("login_prefs", Context.MODE_PRIVATE);
        String token = prefs.getString("jwt", null);
        if (token == null || token.isEmpty()) token = prefs.getString("token", null);
        if (token == null || token.isEmpty()) token = prefs.getString("accessToken", null);
        return (token == null || token.isEmpty()) ? null : "Bearer " + token;
    }

    // ViewHolder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvContent;
        private final TextView tvAuthor;
        private final TextView tvDate;

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
