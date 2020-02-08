package com.weishu.upf.hook_classloader;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.weishu.upf.hook_classloader.classloder_hook.BaseDexClassLoaderHookHelper;
import com.weishu.upf.hook_classloader.classloder_hook.LoadedApkClassLoaderHookHelper;

import java.io.File;

/**
 * @author weishu
 * @date 16/3/28
 */
public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    private static final int PATCH_BASE_CLASS_LOADER = 1;

    private static final int CUSTOM_CLASS_LOADER = 2;

    private static final int HOOK_METHOD = PATCH_BASE_CLASS_LOADER;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Button t = new Button(this);
        t.setText("test button");

        setContentView(t);

        Log.d(TAG, "context classloader: " + getApplicationContext().getClassLoader());
        t.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent();
                    if (HOOK_METHOD == CUSTOM_CLASS_LOADER) {
                        intent.setComponent(new ComponentName("com.weishu.upf.dynamic_proxy_hook.app2",
                                "com.weishu.upf.dynamic_proxy_hook.app2.MainActivity"));
                    } else {
                        intent.setComponent(new ComponentName(
                                "com.weishu.upf.ams_pms_hook.app",
                                "com.weishu.upf.ams_pms_hook.app.MainActivity"));
                    }
                    startActivity(intent);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        try {
            Utils.extractAssets(newBase, "ams-pms-hook-debug.apk");
            Utils.extractAssets(newBase, "dynamic-proxy-hook.apk");
            if (HOOK_METHOD == CUSTOM_CLASS_LOADER) {
                File dexFile = getFileStreamPath("dynamic-proxy-hook.apk");
                File optDexFile = getFileStreamPath("dynamic-proxy-hook.dex");
                BaseDexClassLoaderHookHelper.patchClassLoader(getClassLoader(), dexFile, optDexFile);
            } else {
                LoadedApkClassLoaderHookHelper.hookLoadedApkInActivityThread(getFileStreamPath("ams-pms-hook-debug.apk"));
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
