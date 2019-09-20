package com.weishu.upf.hook_classloader;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.weishu.upf.hook_classloader.classloder_hook.BaseDexClassLoaderHookHelper;
import com.weishu.upf.hook_classloader.classloder_hook.LoadedApkClassLoaderHookHelper;

import java.io.File;
import java.lang.reflect.Method;

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
            Utils.extractAssets(newBase, "ams-pms-hook.apk");
            Utils.extractAssets(newBase, "dynamic-proxy-hook.apk");
            if (HOOK_METHOD == CUSTOM_CLASS_LOADER) {
                File dexFile = getFileStreamPath("dynamic-proxy-hook.apk");
                File optDexFile = getFileStreamPath("dynamic-proxy-hook.dex");
                BaseDexClassLoaderHookHelper.patchClassLoader(getClassLoader(), dexFile, optDexFile);
            } else {
                LoadedApkClassLoaderHookHelper.hookLoadedApkInActivityThread(getFileStreamPath("ams-pms-hook.apk"));
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }


    private AssetManager mAssetManager = null;//资源管理器
    private Resources mResources;//资源
    private Resources.Theme mTheme;//主题

    protected void loadResources(String dexPath) {
        try {
            AssetManager assetManager = AssetManager.class.newInstance();
            Method addAssetPath = assetManager.getClass().getMethod("addAssetPath", String.class);
            addAssetPath.invoke(assetManager, dexPath);
            mAssetManager = assetManager;
        } catch (Exception e) {
            Log.i("inject", "loadResource error:" + Log.getStackTraceString(e));
            e.printStackTrace();
        }
        Resources superRes = super.getResources();
        superRes.getDisplayMetrics();
        superRes.getConfiguration();
        mResources = new Resources(mAssetManager, superRes.getDisplayMetrics(), superRes.getConfiguration());
        mTheme = mResources.newTheme();
        mTheme.setTo(super.getTheme());
    }

    @Override
    public AssetManager getAssets() {
        return mAssetManager == null ? super.getAssets() : mAssetManager;
    }

    @Override
    public Resources getResources() {
        return mResources == null ? super.getResources() : mResources;
    }

    @Override
    public Resources.Theme getTheme() {
        return mTheme == null ? super.getTheme() : mTheme;
    }
}
