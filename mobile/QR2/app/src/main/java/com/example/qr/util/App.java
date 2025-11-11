package com.example.qr.util;

import android.app.Application;
import com.example.qr.api.RetrofitClient;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        RetrofitClient.init(this);
    }
}
