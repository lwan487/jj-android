package com.github.jj.android.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.NonNull;

import me.yugy.app.common.utils.DebugUtils;

public class MessageService extends Service {

    public static void start(Context context) {
        context.startService(new Intent(context, MessageService.class));
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, MessageService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        DebugUtils.log("onCreate");
    }

    @Override
    public int onStartCommand(@NonNull Intent intent, int flags, int startId) {
        handleIntent(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    private void handleIntent(Intent intent) {
        if (intent != null) {
            DebugUtils.log("handleIntent");
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        DebugUtils.log("onTaskRemoved");
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        DebugUtils.log("onDestroy");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(@NonNull Intent intent) {
        return null;
    }
}
