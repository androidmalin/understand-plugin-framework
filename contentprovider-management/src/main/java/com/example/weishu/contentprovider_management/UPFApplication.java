package com.example.weishu.contentprovider_management;

import android.app.Application;
import android.content.Context;

import com.example.weishu.contentprovider_management.hook.BaseDexClassLoaderHookHelper;

import java.io.File;

/**
 * 一定需要Application，并且在attachBaseContext里面Hook
 * 因为provider的初始化非常早，比Application的onCreate还要早
 * 在别的地方hook都晚了。
 *
 * @author weishu
 * 16/3/29
 */
public class UPFApplication extends Application {

    private static final String PLUGIN_APK = "contentPlugin-debug.apk";
    private static final String PLUGIN_DEX = "contentPlugin-debug.dex";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        try {
            File apkFile = getFileStreamPath(PLUGIN_APK);
            if (!apkFile.exists()) {
                Utils.extractAssets(base, PLUGIN_APK);
            }
            File odexFile = getFileStreamPath(PLUGIN_DEX);
            // Hook ClassLoader, 让插件中的类能够被成功加载
            BaseDexClassLoaderHookHelper.patchClassLoader(getClassLoader(), apkFile, odexFile);
            ProviderHelper.installProviders(base, getFileStreamPath(PLUGIN_APK));
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

}
