package com.mobile.greenacademypartner.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ForceLogoutReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (ForceLogoutHelper.ACTION_FORCE_LOGOUT.equals(intent.getAction())) {
            String reason = intent.getStringExtra("reason");
            ForceLogoutHelper.forceLogout(context.getApplicationContext(), reason);
        }
    }
}
