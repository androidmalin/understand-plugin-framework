package com.weishu.upf.hook_classloader.ams_hook;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author weishu
 * @date 16/1/7
 */
@SuppressLint("PrivateApi")
class ActivityThreadHandlerCallback implements Handler.Callback {

    private static final String TAG = "ActivityThreadHand";

    public ActivityThreadHandlerCallback() {
    }

    @Override
    public boolean handleMessage(Message msg) {

        switch (msg.what) {
            // ActivityThread里面 "LAUNCH_ACTIVITY" 这个字段的值是100
            // 本来使用反射的方式获取最好, 这里为了简便直接使用硬编码
            case 100:
                handleLaunchActivity(msg);
                break;
        }
        return false;
    }

    private void handleLaunchActivity(Message msg) {
        // 这里简单起见,直接取出TargetActivity;

        Object activityClientRecordObj = msg.obj;
        // 根据源码:
        // 这个对象是 ActivityClientRecord 类型
        // 我们修改它的intent字段为我们原来保存的即可.
        // switch (msg.what) {
        //      case LAUNCH_ACTIVITY: {
        //          Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "activityStart");
        //          final ActivityClientRecord r = (ActivityClientRecord) msg.obj;

        //          r.packageInfo = getPackageInfoNoCheck(
        //                  r.activityInfo.applicationInfo, r.compatInfo);
        //         handleLaunchActivity(r, null);

        try {
            // 把替身恢复成真身
            //1.获取安全的Intent
            Field intent = activityClientRecordObj.getClass().getDeclaredField("intent");
            intent.setAccessible(true);
            Intent safeIntent = (Intent) intent.get(activityClientRecordObj);

            if (safeIntent == null) return;
            //获取原始Intent
            Intent target = safeIntent.getParcelableExtra(AMSHookHelper.EXTRA_TARGET_INTENT);
            if (target == null || target.getComponent() == null) return;
            safeIntent.setComponent(target.getComponent());

            Field activityInfoField = activityClientRecordObj.getClass().getDeclaredField("activityInfo");
            activityInfoField.setAccessible(true);

            //TODO:注意这里.....
            // 根据 getPackageInfo 根据这个 包名获取 LoadedApk的信息; 因此这里我们需要手动填上, 从而能够命中缓存
            ActivityInfo activityInfo = (ActivityInfo) activityInfoField.get(activityClientRecordObj);
            activityInfo.applicationInfo.packageName = target.getPackage() == null ?
                    target.getComponent().getPackageName() : target.getPackage();

            hookPackageManager();
        } catch (Throwable e) {
            Log.d(TAG, e.getMessage(), e);
        }
    }

    /**
     * 我们的插件并没有安装在系统上，因此系统肯定认为插件没有安装
     * 所以，我们还要欺骗一下PMS，让系统觉得插件已经安装在系统上了
     */
    private static void hookPackageManager() throws Exception {

        // 这一步是因为 initializeJavaContextClassLoader 这个方法内部无意中检查了这个包是否在系统安装
        // 如果没有安装, 直接抛出异常, 这里需要临时Hook掉 PMS, 绕过这个检查.

        //1.ActivityThread
        Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
        Method currentActivityThreadMethod = activityThreadClass.getDeclaredMethod("currentActivityThread");
        currentActivityThreadMethod.setAccessible(true);
        Object currentActivityThread = currentActivityThreadMethod.invoke(null);

        //2. 获取ActivityThread里面原始的 sPackageManager
        Field sPackageManagerField = activityThreadClass.getDeclaredField("sPackageManager");
        sPackageManagerField.setAccessible(true);
        Object sPackageManager = sPackageManagerField.get(currentActivityThread);

        //3.准备好代理对象, 用来替换原始的对象
        Class<?> iPackageManagerInterface = Class.forName("android.content.pm.IPackageManager");

        Object proxy = Proxy.newProxyInstance(iPackageManagerInterface.getClassLoader(),
                new Class<?>[]{iPackageManagerInterface},
                new IPackageManagerHookHandler(sPackageManager));

        //4.替换掉ActivityThread里面的 sPackageManager 字段
        sPackageManagerField.set(currentActivityThread, proxy);
    }

    public static class IPackageManagerHookHandler implements InvocationHandler {

        private Object mBase;

        public IPackageManagerHookHandler(Object base) {
            mBase = base;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getName().equals("getPackageInfo")) {
                return new PackageInfo();
            }
            return method.invoke(mBase, args);
        }
    }
}
