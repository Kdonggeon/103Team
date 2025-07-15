package com.mobile.greenacademypartner.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.ui.setting.ThemeColorUtil;

public class FindIdResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_id_result);

        TextView textFoundId = findViewById(R.id.text_found_id);
        Button btnBack = findViewById(R.id.btn_back_to_login);

        String userId = getIntent().getStringExtra("userId");
        textFoundId.setText("아이디: " + userId);

        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(FindIdResultActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
        ThemeColorUtil.applyThemeColor(this);
    }
}
