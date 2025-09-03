// BackendGuardInterceptor.java
package com.mobile.greenacademypartner.net;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Build;

import com.mobile.greenacademypartner.core.ForceLogoutHelper;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class BackendGuardInterceptor implements Interceptor {

    private final Context appContext;

    public BackendGuardInterceptor(Context context) {
        this.appContext = context.getApplicationContext();
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        // 1) 오프라인이면 강제 로그아웃
        if (!isOnline()) {
            sendForceLogout("인터넷 연결이 필요합니다. 다시 로그인해 주세요.");
            throw new IOException("Offline - force logout");
        }

        Request req = chain.request();

        try {
            Response res = chain.proceed(req);

            // 2) 인증 만료/권한 문제/서버 다운 등 응답 코드에 따라 강제 로그아웃
            int code = res.code();
            if (code == 401 || code == 403) {
                // 인증 만료
                sendForceLogout("세션이 만료되었어요. 다시 로그인해 주세요.");
            } else if (code == 503) {
                // 서버 점검/다운
                sendForceLogout("서버 점검 중입니다. 다시 로그인해 주세요.");
            }
            return res;
        } catch (IOException ioe) {
            // 3) 요청 자체 실패(서버 미기동/네트워크 오류)
            sendForceLogout("서버에 연결할 수 없습니다. 다시 로그인해 주세요.");
            throw ioe;
        }
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.net.Network network = cm.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            return caps != null && (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                    || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            @SuppressWarnings("deprecation")
            android.net.NetworkInfo ni = cm.getActiveNetworkInfo();
            return ni != null && ni.isConnected();
        }
    }

    private void sendForceLogout(String reason) {
        // 브로드캐스트 방식
        Intent i = new Intent(ForceLogoutHelper.ACTION_FORCE_LOGOUT);
        i.putExtra("reason", reason);
        appContext.sendBroadcast(i);

        // 또는 바로 호출 (브로드캐스트 없이)
        // ForceLogoutHelper.forceLogout(appContext, reason);
    }
}
