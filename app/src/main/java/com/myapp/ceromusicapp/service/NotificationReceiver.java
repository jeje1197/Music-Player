package com.myapp.ceromusicapp.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        context.sendBroadcast(new Intent("NOTIFICATION_FILTER")
                .putExtra("ACTION_NAME", intent.getAction()));
    }
}
