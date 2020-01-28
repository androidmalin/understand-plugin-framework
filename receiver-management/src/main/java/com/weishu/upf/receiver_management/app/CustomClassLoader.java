package com.weishu.upf.receiver_management.app;

import android.util.Log;

import java.io.File;

import dalvik.system.DexClassLoader;

/**
 * @author weishu
 * 16/4/7
 */
public class CustomClassLoader extends DexClassLoader {
    private static final String TAG = "CustomClassLoader";

    public CustomClassLoader(String dexPath, String optimizedDirectory, String libraryPath, ClassLoader parent) {
        super(dexPath, optimizedDirectory, libraryPath, parent);
    }

    /**
     * 便利方法: 获取插件的ClassLoader, 能够加载指定的插件中的类
     */
    public static CustomClassLoader getPluginClassLoader(File plugin, String packageName) {
        //String dexPath, String optimizedDirectory, String librarySearchPath, ClassLoader parent
        Log.d(TAG, "dexPath:" + plugin.getPath());
        Log.d(TAG, "optimizedDirectory:" + Utils.getPluginOptDexDir(packageName).getPath());
        Log.d(TAG, "librarySearchPath:" + Utils.getPluginLibDir(packageName).getPath());
        Log.d(TAG, "parent ClassLoader:" + UPFApplication.getContext().getClassLoader().getClass().getCanonicalName());
        return new CustomClassLoader(plugin.getPath(),
                Utils.getPluginOptDexDir(packageName).getPath(),
                Utils.getPluginLibDir(packageName).getPath(),
                UPFApplication.getContext().getClassLoader());
    }

}
