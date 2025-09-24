package com.mobile.greenacademypartner.ui.main;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.ui.menu.MainMenuAdapter;
import com.mobile.greenacademypartner.menu.NavigationMenuHelper;

public class MainDashboardActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_dashboard);

        Toolbar tb = findViewById(R.id.toolbar_main_dashboard);
        setSupportActionBar(tb);

        RecyclerView rv = findViewById(R.id.recycler_main_menu_list);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new MainMenuAdapter(this,
                NavigationMenuHelper.labels,
                NavigationMenuHelper.getIconsLight(),
                NavigationMenuHelper.getIconsDark(),
                NavigationMenuHelper.getTargetActivities()
        ));
    }
}