package com.weishu.upf.receiver_management.app;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

/**
 * 这个类只是为了方便获取全局Context的.
 *
 * @author weishu
 * 16/3/29
 */
public class UPFApplication extends Application {

    @SuppressLint("StaticFieldLeak")
    private static Context sContext;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        sContext = base;
    }

    public static Context getContext() {
        return sContext;
    }
}
