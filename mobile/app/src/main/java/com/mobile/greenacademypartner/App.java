package com.mobile.greenacademypartner;

import android.app.Application;
import com.mobile.greenacademypartner.api.RetrofitClient;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // 전역 Retrofit 초기화 (인터셉터들이 쓸 앱 컨텍스트 주입)
        RetrofitClient.init(this);
    }
}
