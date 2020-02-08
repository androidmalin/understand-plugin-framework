package com.weishu.upf.hook_classloader;

import android.app.Application;
import android.content.Context;
import android.os.Build;

import com.weishu.upf.hook_classloader.ams_hook.AMSHookHelper;

import me.weishu.reflection.Reflection;


/**
 * 这个类只是为了方便获取全局Context的.
 *
 * @author weishu
 * @date 16/3/29
 */
public class UPFApplication extends Application {

    private static Context sContext;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        if (Build.VERSION.SDK_INT >= 28) {
            Reflection.unseal(base);
        }
//        try {
//            AMSHookHelper.hookActivityManagerNative();
//            AMSHookHelper.hookActivityThreadHandler();
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        } catch (NoSuchFieldException e) {
//            e.printStackTrace();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        HookActivity.hookStartActivity(base, StubActivity.class);
        HookActivity.hookLauncherActivity(base, StubActivity.class, true);
        sContext = base;
    }

    public static Context getContext() {
        return sContext;
    }
}
