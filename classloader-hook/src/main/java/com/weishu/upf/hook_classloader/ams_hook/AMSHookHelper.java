package com.weishu.upf.hook_classloader.ams_hook;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import com.weishu.upf.hook_classloader.StubActivity;
import com.weishu.upf.hook_classloader.UPFApplication;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author weishu
 * @date 16/3/21
 */
@SuppressWarnings("JavaReflectionMemberAccess")
@SuppressLint("PrivateApi")
public class AMSHookHelper {

    public static final String EXTRA_TARGET_INTENT = "extra_target_intent";

    /**
     * Hook AMS
     * <p/>
     * 主要完成的操作是  "把真正要启动的Activity临时替换为在AndroidManifest.xml中声明的替身Activity"
     * <p/>
     * 进而骗过AMS
     */
    public static void hookActivityManagerNative() throws ClassNotFoundException, IllegalAccessException, NoSuchFieldException {


        Object gDefault;
        if (Build.VERSION.SDK_INT >= 26) {
            //1.获取ActivityManager的Class对象
            //package android.app
            //public class ActivityManager
            Class<?> activityManagerClass = Class.forName("android.app.ActivityManager");

            //2.获取ActivityManager的私有静态属性IActivityManagerSingleton
            //private static final Singleton<IActivityManager> IActivityManagerSingleton
            Field iActivityManagerSingletonField = activityManagerClass.getDeclaredField("IActivityManagerSingleton");

            //3.取消Java的权限检查
            iActivityManagerSingletonField.setAccessible(true);

            //4.获取IActivityManagerSingleton的实例对象
            //private static final Singleton<IActivityManager> IActivityManagerSingleton
            //所有静态对象的反射可以通过传null获取,如果是非静态必须传实例
            gDefault = iActivityManagerSingletonField.get(null);
        } else {
            //1.获取ActivityManagerNative的Class对象
            //package android.app
            //public abstract class ActivityManagerNative
            Class<?> activityManagerNativeClass = Class.forName("android.app.ActivityManagerNative");

            //2.获取 ActivityManagerNative的 私有属性gDefault
            // private static final Singleton<IActivityManager> gDefault
            Field singletonField = activityManagerNativeClass.getDeclaredField("gDefault");

            //3.对私有属性gDefault,解除私有限定
            singletonField.setAccessible(true);

            //4.获得gDefaultField中对应的属性值(被static修饰了),既得到Singleton<IActivityManager>对象的实例
            //所有静态对象的反射可以通过传null获取
            //private static final Singleton<IActivityManager> gDefault
            gDefault = singletonField.get(null);
        }


        //4. gDefault是一个 android.util.Singleton对象; 我们取出这个单例里面的字段
        Class<?> singleton = Class.forName("android.util.Singleton");
        Field mInstanceField = singleton.getDeclaredField("mInstance");
        mInstanceField.setAccessible(true);

        //5.获取ActivityManagerNative 的gDefault对象里面原始的 IActivityManager对象
        Object rawIActivityManager = mInstanceField.get(gDefault);

        //6.创建一个这个对象的代理对象, 然后替换这个字段, 让我们的代理对象帮忙干活
        Class<?> iActivityManagerInterface = Class.forName("android.app.IActivityManager");

        Object proxy = Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class<?>[]{iActivityManagerInterface},
                new IActivityManagerHandler(rawIActivityManager));

        //7.
        mInstanceField.set(gDefault, proxy);

    }

    @SuppressWarnings("ConstantConditions")
    private static class IActivityManagerHandler implements InvocationHandler {
        private static final String TAG = "IActivityManagerHandler";
        private Object iActivityManagerObj;

        public IActivityManagerHandler(Object base) {
            iActivityManagerObj = base;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            if ("startActivity".equals(method.getName())) {
                // 只拦截这个方法
                // 替换参数, 任你所为;甚至替换原始Activity启动别的Activity偷梁换柱
                // API 23:
                // public final Activity startActivityNow(Activity parent, String id,
                // Intent intent, ActivityInfo activityInfo, IBinder token, Bundle state,
                // Activity.NonConfigurationInstances lastNonConfigurationInstances) {

                //1. 找到参数里面的第一个Intent 对象
                Intent raw;
                int index = 0;
                for (int i = 0; i < args.length; i++) {
                    if (args[i] instanceof Intent) {
                        index = i;
                        break;
                    }
                }
                raw = (Intent) args[index];

                //2.创建安全的Intent
                Intent newIntent = new Intent();

                // 替身Activity的包名, 也就是我们自己的"包名", Application Id, 如果用gradle打包
                String stubPackage = UPFApplication.getContext().getPackageName();

                // 这里我们把启动的Activity临时替换为 StubActivity
                ComponentName componentName = new ComponentName(stubPackage, StubActivity.class.getName());

                //3.安全的Intent设置数据
                newIntent.setComponent(componentName);

                //4.把我们原始要启动的TargetActivity先存起来
                newIntent.putExtra(AMSHookHelper.EXTRA_TARGET_INTENT, raw);

                //5.替换掉Intent, 达到欺骗AMS的目的
                args[index] = newIntent;
                Log.d(TAG, "hook success");
                return method.invoke(iActivityManagerObj, args);
            }
            return method.invoke(iActivityManagerObj, args);
        }
    }


    /**
     * 由于之前我们用替身欺骗了AMS; 现在我们要换回我们真正需要启动的Activity
     * <p/>
     * 不然就真的启动替身了, 狸猫换太子...
     * <p/>
     * 到最终要启动Activity的时候,会交给ActivityThread 的一个内部类叫做 H 来完成
     * H 会完成这个消息转发; 最终调用它的callback
     */
    public static void hookActivityThreadHandler() throws Exception {

        // 1.先获取到当前的ActivityThread对象
        Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
        Method currentActivityThreadMethod = activityThreadClass.getDeclaredMethod("currentActivityThread");
        currentActivityThreadMethod.setAccessible(true);
        Object currentActivityThread = currentActivityThreadMethod.invoke(null);

        //2.由于ActivityThread一个进程只有一个,我们获取这个对象的mH
        Field mHField = activityThreadClass.getDeclaredField("mH");
        mHField.setAccessible(true);
        Handler mH = (Handler) mHField.get(currentActivityThread);

        //3.
        Field mCallBackField = Handler.class.getDeclaredField("mCallback");
        mCallBackField.setAccessible(true);

        mCallBackField.set(mH, new ActivityThreadHandlerCallback());

    }

}
