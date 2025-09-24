package com.mobile.greenacademypartner.ui.menu;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.mobile.greenacademypartner.R;

public class MainMenuAdapter extends RecyclerView.Adapter<MainMenuAdapter.VH> {

    private final Activity activity;
    private final String[] labels;
    private final int[] iconsLight;
    private final int[] iconsDark;
    private final Class<?>[] targets;

    public MainMenuAdapter(Activity activity, String[] labels, int[] iconsLight, int[] iconsDark, Class<?>[] targets) {
        this.activity = activity;
        this.labels = labels;
        this.iconsLight = iconsLight;
        this.iconsDark = iconsDark;
        this.targets = targets;
    }

    static class VH extends RecyclerView.ViewHolder {
        MaterialCardView card;
        ImageView icon;
        TextView label;
        VH(@NonNull View v) {
            super(v);
            card  = v.findViewById(R.id.card_main_menu_item);
            icon  = v.findViewById(R.id.iv_menu_icon);
            label = v.findViewById(R.id.tv_menu_label);
        }
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_main_menu, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        h.label.setText(labels[pos]);
        h.icon.setImageResource(iconsLight[pos]);

        h.card.setOnClickListener(v -> {
            if (targets[pos] == null) return;
            boolean isSame = activity.getClass().equals(targets[pos]);
            boolean isAttendance = "AttendanceActivity".equals(targets[pos].getSimpleName());
            if (isAttendance || !isSame) {
                Intent intent = new Intent(activity, targets[pos]);
                activity.startActivity(intent);
            }
        });

        // 포커스/프레스(리플) 등은 XML 스타일로 처리
    }

    @Override public int getItemCount() { return labels.length; }
}