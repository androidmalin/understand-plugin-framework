package com.weishu.upf.demo.app2;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class TargetService1 extends Service {

    private static final String TAG = "TargetService1";

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate() called with ");
        super.onCreate();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onStart(Intent intent, int startId) {
        Log.d(TAG, "onStart() called with intent = [" + intent + "], startId = [" + startId + "]");
        super.onStart(intent, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy() called with ");
        super.onDestroy();
    }
}
