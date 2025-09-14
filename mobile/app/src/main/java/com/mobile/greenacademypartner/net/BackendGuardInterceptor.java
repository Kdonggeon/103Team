package com.mobile.greenacademypartner.net;

import android.content.Context;

import com.mobile.greenacademypartner.util.SessionUtil;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class BackendGuardInterceptor implements Interceptor {

    private final Context appContext;

    public BackendGuardInterceptor(Context appContext) {
        this.appContext = appContext.getApplicationContext();
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        if (!SessionUtil.isNetworkAvailable(appContext)) {
            throw new IOException("No network connectivity");
        }

        Request req = chain.request();
        Response res = chain.proceed(req);

        int code = res.code();
        if (code == 401 || code == 403) {
            SessionUtil.clearLoginAndGoLogin(appContext, "AUTH_EXPIRED");
        } else if (code == 503) {
            SessionUtil.clearLoginAndGoLogin(appContext, "SERVER_UNAVAILABLE");
        }
        return res;
    }

}
