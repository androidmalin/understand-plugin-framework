package com.weishu.upf.hook_classloader;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * 对Activity启动流程中的两次拦截
 */
class HookActivity {

    private static final String EXTRA_ORIGIN_INTENT = "EXTRA_ORIGIN_INTENT";


    /**
     * 对IActivityManager接口中的startActivity方法进行动态代理,发生在app的进程中
     * {@link android.app.Activity#startActivity(Intent)}
     * {@link android.app.Activity#startActivityForResult(Intent, int, Bundle)}
     * android.app.Instrumentation#execStartActivity()
     * Activity#startActivityForResult-->Instrumentation#execStartActivity-->ActivityManager.getService().startActivity()-->
     * IActivityManager public int startActivity(android.app.IApplicationThread caller, java.lang.String callingPackage, android.content.Intent intent, java.lang.String resolvedType, android.os.IBinder resultTo, java.lang.String resultWho, int requestCode, int flags, android.app.ProfilerInfo profilerInfo, android.os.Bundle options) throws android.os.RemoteException;
     *
     * @param context          context
     * @param subActivityClass 在AndroidManifest.xml中注册了的Activity
     */
    @SuppressWarnings({"JavaReflectionMemberAccess", "PrivateApi"})
    static void hookStartActivity(Context context, Class<?> subActivityClass) {

        try {
            Object IActivityManagerSingletonObj;
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
                IActivityManagerSingletonObj = iActivityManagerSingletonField.get(null);
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
                IActivityManagerSingletonObj = singletonField.get(null);
            }


            //5.获取private static final Singleton<IActivityManager> IActivityManagerSingleton对象中的属性private T mInstance的值
            //既,为了获取一个IActivityManager的实例对象
            //private static final Singleton<IActivityManager> IActivityManagerSingleton =new Singleton<IActivityManager>(){...}


            //6.获取Singleton类对象
            //package android.util
            //public abstract class Singleton<T>
            Class<?> singletonClass = Class.forName("android.util.Singleton");

            //7.获取mInstance属性
            //private T mInstance;
            Field mInstanceField = singletonClass.getDeclaredField("mInstance");

            //8.取消Java的权限检查
            mInstanceField.setAccessible(true);

            //9.获取mInstance属性的值,既IActivityManager的实例
            //从private static final Singleton<IActivityManager> IActivityManagerSingleton实例对象中获取mInstance属性对应的值,既IActivityManager
            Object iActivityManager = mInstanceField.get(IActivityManagerSingletonObj);


            //10.获取IActivityManager接口的类对象
            //package android.app
            //public interface IActivityManager
            Class<?> iActivityManagerClass = Class.forName("android.app.IActivityManager");

            //11.创建一个IActivityManager接口的代理对象
            Object iActivityManagerProxy = Proxy.newProxyInstance(
                    Thread.currentThread().getContextClassLoader(),
                    new Class[]{iActivityManagerClass},
                    new IActivityInvocationHandler(iActivityManager, context, subActivityClass)
            );

            //11.重新赋值
            //给mInstance属性,赋新值
            //给Singleton<IActivityManager> IActivityManagerSingleton实例对象的属性private T mInstance赋新值
            mInstanceField.set(IActivityManagerSingletonObj, iActivityManagerProxy);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    /**
     * 对IActivityManager接口进行动态代理
     */
    private static class IActivityInvocationHandler implements InvocationHandler {

        private Object mIActivityManager;
        private Class<?> mSubActivityClass;
        private Context mContext;


        private IActivityInvocationHandler(Object iActivityManager, Context context, Class<?> subActivityClass) {
            this.mIActivityManager = iActivityManager;
            this.mSubActivityClass = subActivityClass;
            this.mContext = context;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
            // public int startActivity(IApplicationThread caller, String callingPackage, Intent intent,String resolvedType, IBinder resultTo, String resultWho, int requestCode, int flags,ProfilerInfo profilerInfo, Bundle options)
            if (method.getName().equals("startActivity")) {
                int intentIndex = 2;
                for (int i = 0; i < args.length; i++) {
                    if (args[i] instanceof Intent) {
                        intentIndex = i;
                        break;
                    }
                }
                //将启动的未注册的Activity对应的Intent,替换为安全的注册了的桩Activity的Intent
                //1.将未注册的Activity对应的Intent,改为安全的Intent,既在AndroidManifest.xml中配置了的桩Activity的Intent
                Intent originIntent = (Intent) args[intentIndex];

                Intent safeIntent = new Intent(mContext, mSubActivityClass);
                //public class Intent implements Parcelable;
                //Intent类已经实现了Parcelable接口
                safeIntent.putExtra(EXTRA_ORIGIN_INTENT, originIntent);

                //2.替换到原来的Intent,欺骗AMS
                args[intentIndex] = safeIntent;

                //3.之后,再换回来,启动我们未在AndroidManifest.xml中配置的Activity
                //final H mH = new H();
                //hook Handler消息的处理,给Handler增加mCallback


            }
            //public abstract int android.app.IActivityManager.startActivity(android.app.IApplicationThread,java.lang.String,android.content.Intent,java.lang.String,android.os.IBinder,java.lang.String,int,int,android.app.ProfilerInfo,android.os.Bundle) throws android.os.RemoteException
            return method.invoke(mIActivityManager, args);
        }
    }


    /**
     * 启动未注册的Activity,将之前替换了的Intent,换回去.我们的目标是要启动未注册的Activity
     *
     * @param context          context
     * @param subActivityClass 注册了的Activity的Class对象
     * @param isAppCompat      是否是AppCompatActivity的子类
     */
    @SuppressWarnings({"JavaReflectionMemberAccess", "DiscouragedPrivateApi", "PrivateApi"})
    static void hookLauncherActivity(Context context, Class<?> subActivityClass, boolean isAppCompat) {

        try {
            //1.获取ActivityThread的Class对象
            //package android.app
            //public final class ActivityThread
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");

            //2.获取currentActivityThread()静态方法;为了保证在多个版本中兼容性,使用该静态方法获取ActivityThread的实例
            //public static ActivityThread currentActivityThread(){return sCurrentActivityThread;}
            Method currentActivityThreadMethod = activityThreadClass.getDeclaredMethod("currentActivityThread");
            currentActivityThreadMethod.setAccessible(true);

            //3.获取ActivityThread的对象实例
            //public static ActivityThread currentActivityThread(){return sCurrentActivityThread;}
            Object activityThreadObj = currentActivityThreadMethod.invoke(null);


            //4.获取ActivityThread 的属性mH
            //final H mH = new H();
            Field mHField = activityThreadClass.getDeclaredField("mH");
            mHField.setAccessible(true);

            //5.获取mH的值,既获取ActivityThread类中H类的实例对象
            //从ActivityThread实例中获取mH属性对应的值,既mH的值
            Object mHObj = mHField.get(activityThreadObj);


            //6.获取Handler的Class对象
            //package android.os
            //public class Handler
            Class<?> handlerClass = Class.forName("android.os.Handler");


            //7.获取mCallback属性
            //final Callback mCallback;
            //Callback是Handler类内部的一个接口
            Field mCallbackField = handlerClass.getDeclaredField("mCallback");
            mCallbackField.setAccessible(true);


            //8.给mH增加mCallback
            //给mH,既Handler的子类设置mCallback属性,提前对消息进行处理.
            if (Build.VERSION.SDK_INT >= 28) {
                //android 9.0
                mCallbackField.set(mHObj, new HandlerCallbackP(context, subActivityClass, isAppCompat));
            } else {
                mCallbackField.set(mHObj, new HandlerCallback(context, subActivityClass, isAppCompat));
            }
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 对应<9.0情况,创建一个Handler的Callback接口的实例对象
     */
    private static class HandlerCallback implements Handler.Callback {
        private Context context;
        private Class<?> subActivityClass;
        private boolean isAppCompat;

        private HandlerCallback(Context context, Class<?> subActivityClass, boolean isAppCompat) {
            this.context = context;
            this.subActivityClass = subActivityClass;
            this.isAppCompat = isAppCompat;
        }

        @Override
        public boolean handleMessage(Message msg) {
            handleLaunchActivity(msg, context, subActivityClass, isAppCompat);
            return false;
        }
    }


    @SuppressLint("PrivateApi")
    private static void handleLaunchActivity(Message msg, Context context, Class<?> subActivityClass, boolean isAppCompat) {
        int LAUNCH_ACTIVITY = 100;
        try {
            //1.获取ActivityThread的内部类H的Class对象
            //package android.app
            //public final class ActivityThread{
            //       private class H extends Handler {}
            //}
            Class<?> hClass = Class.forName("android.app.ActivityThread$H");

            //2.获取LAUNCH_ACTIVITY属性的Field
            // public static final int LAUNCH_ACTIVITY = 100;
            Field launch_activity_field = hClass.getField("LAUNCH_ACTIVITY");

            //3.获取LAUNCH_ACTIVITY的值
            LAUNCH_ACTIVITY = (int) launch_activity_field.get(null);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

        if (msg.what != LAUNCH_ACTIVITY) return;
        // private class H extends Handler {
        // public void handleMessage(Message msg) {
        //            switch (msg.what) {
        //                case LAUNCH_ACTIVITY: {
        //                    final ActivityClientRecord r = (ActivityClientRecord) msg.obj;
        //                    r.packageInfo = getPackageInfoNoCheck(r.activityInfo.applicationInfo, r.compatInfo);
        //                    handleLaunchActivity(r, null, "LAUNCH_ACTIVITY");
        //                    break;
        //                }
        //            }
        //    }
        //1.从msg中获取ActivityClientRecord对象
        //android.app.ActivityThread$ActivityClientRecord
        //static final class ActivityClientRecord {}
        Object activityClientRecordObj = msg.obj;

        try {
            //2.获取ActivityClientRecord的intent属性
            // Intent intent;
            Field safeIntentField = activityClientRecordObj.getClass().getDeclaredField("intent");
            safeIntentField.setAccessible(true);

            //3.获取ActivityClientRecord的intent属性的值,既安全的Intent
            Intent safeIntent = (Intent) safeIntentField.get(activityClientRecordObj);
            if (safeIntent == null) return;

            //4.获取原始的Intent
            Intent originIntent = safeIntent.getParcelableExtra(EXTRA_ORIGIN_INTENT);

            if (originIntent == null) return;

            //5.将安全的Intent,替换为原始的Intent,以启动我们要启动的未注册的Activity
            safeIntent.setComponent(originIntent.getComponent());

            //6.处理启动的Activity为AppCompatActivity类或者子类的情况
            if (!isAppCompat) return;
            hookPackageManager(context, subActivityClass);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }


    /**
     * 对Android 9.0的处理
     * https://www.cnblogs.com/Jax/p/9521305.html
     */
    @SuppressLint("PrivateApi")
    private static class HandlerCallbackP implements Handler.Callback {

        private Context context;
        private Class<?> subActivityClass;
        private boolean isAppCompat;

        private HandlerCallbackP(Context context, Class<?> subActivityClass, boolean isAppCompat) {
            this.context = context;
            this.subActivityClass = subActivityClass;
            this.isAppCompat = isAppCompat;
        }

        @Override
        public boolean handleMessage(Message msg) {
            //android.app.ActivityThread$H.EXECUTE_TRANSACTION = 159
            //android 9.0反射,Accessing hidden field Landroid/app/ActivityThread$H;->EXECUTE_TRANSACTION:I (dark greylist, reflection)
            //android9.0 深灰名单（dark greylist）则debug版本在会弹出dialog提示框，在release版本会有Toast提示，均提示为"Detected problems with API compatibility"
            if (msg.what == 159) {//直接写死,不反射了,否则在android9.0的设备上运行会弹出使用了反射的dialog提示框
                handleActivity(msg);
            }
            return false;
        }

        private void handleActivity(Message msg) {

            try {
                // 这里简单起见,直接取出TargetActivity;
                //final ClientTransaction transaction = (ClientTransaction) msg.obj;
                //1.获取ClientTransaction对象
                Object clientTransactionObj = msg.obj;
                if (clientTransactionObj == null) return;


                //2.获取ClientTransaction类中属性mActivityCallbacks的Field
                //private List<ClientTransactionItem> mActivityCallbacks;
                Field mActivityCallbacksField = clientTransactionObj.getClass().getDeclaredField("mActivityCallbacks");

                //3.禁止Java访问检查
                mActivityCallbacksField.setAccessible(true);

                //4.获取ClientTransaction类中mActivityCallbacks属性的值,既List<ClientTransactionItem>
                List<?> mActivityCallbacks = (List<?>) mActivityCallbacksField.get(clientTransactionObj);

                if (mActivityCallbacks == null || mActivityCallbacks.size() <= 0) return;
                if (mActivityCallbacks.get(0) == null) return;


                //5.ClientTransactionItem的Class对象
                //package android.app.servertransaction;
                //public class LaunchActivityItem extends ClientTransactionItem
                Class<?> launchActivityItemClass = Class.forName("android.app.servertransaction.LaunchActivityItem");

                //6.判断集合中第一个元素的值是LaunchActivityItem类型的
                if (!launchActivityItemClass.isInstance(mActivityCallbacks.get(0))) return;

                //7.获取LaunchActivityItem的实例
                // public class LaunchActivityItem extends ClientTransactionItem
                Object launchActivityItem = mActivityCallbacks.get(0);


                //8.ClientTransactionItem的mIntent属性的mIntent的Field
                //private Intent mIntent;
                Field mIntentField = launchActivityItemClass.getDeclaredField("mIntent");

                //9.禁止Java访问检查
                mIntentField.setAccessible(true);

                //10.获取mIntent属性的值,既桩Intent(安全的Intent)
                //从LaunchActivityItem中获取属性mIntent的值
                Intent safeIntent = (Intent) mIntentField.get(launchActivityItem);
                if (safeIntent == null) return;

                //11.获取原始的Intent
                Intent originIntent = safeIntent.getParcelableExtra(EXTRA_ORIGIN_INTENT);

                //12.需要判断originIntent != null
                if (originIntent == null) return;

                //13.将原始的Intent,赋值给clientTransactionItem的mIntent属性
                safeIntent.setComponent(originIntent.getComponent());

                //14.处理未注册的Activity为AppCompatActivity类或者子类的情况
                if (!isAppCompat) return;
                hookPackageManager(context, subActivityClass);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 获取包名
     */
    private static String getAppPackageName(Context context) {
        Context applicationContext = context.getApplicationContext();
        return applicationContext.getPackageName();
    }

     /*1.处理使用com.android.support:appcompat-v7:27.1.1会出现这个问题.使用28.0.0则不会
     https://blog.csdn.net/gdutxiaoxu/article/details/81459910

     对support中android.support.v4.app.NavUtils类的
     public static String getParentActivityName(@NonNull Context context,@NonNull ComponentName componentName){
     PackageManager pm = context.getPackageManager();
     //这里检测到了ComponentInfo{com.malin.hook/com.malin.hook.TargetAppCompatActivity}
     //TargetAppCompatActivity没有在AndroidManifest.xml中注册
     ActivityInfo info = pm.getActivityInfo(componentName, PackageManager.GET_META_DATA)

     Caused by: android.content.pm.PackageManager$NameNotFoundException: ComponentInfo{com.malin.hook/com.malin.hook.TargetAppCompatActivity}
        at android.app.ApplicationPackageManager.getActivityInfo(ApplicationPackageManager.java:418)
        at android.support.v4.app.NavUtils.getParentActivityName(NavUtils.java:240)
        at android.support.v4.app.NavUtils.getParentActivityName(NavUtils.java:219)
        at android.support.v7.app.AppCompatDelegateImplV9.onCreate(AppCompatDelegateImplV9.java:155) 
        at android.support.v7.app.AppCompatDelegateImplV14.onCreate(AppCompatDelegateImplV14.java:61) 
        at android.support.v7.app.AppCompatActivity.onCreate(AppCompatActivity.java:72) 
        at com.malin.hook.TargetAppCompatActivity.onCreate(TargetAppCompatActivity.java:19) 
        at android.app.Activity.performCreate(Activity.java:7009) 
        at android.app.Activity.performCreate(Activity.java:7000) 
        at android.app.Instrumentation.callActivityOnCreate(Instrumentation.java:1214) 
        at android.app.ActivityThread.performLaunchActivity(ActivityThread.java:2731) 
        at android.app.ActivityThread.handleLaunchActivity(ActivityThread.java:2856) 
        at android.app.ActivityThread.-wrap11(Unknown Source:0) 
        at android.app.ActivityThread$H.handleMessage(ActivityThread.java:1589) 
        at android.os.Handler.dispatchMessage(Handler.java:106) 
        at android.os.Looper.loop(Looper.java:164) 
        at android.app.ActivityThread.main(ActivityThread.java:6494) 
        at java.lang.reflect.Method.invoke(Native Method) 
        at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:438) 
        at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:807) 
     **/

    /*2.处理android 4.3以下(<= 18)启动Activity,在ApplicationPackageManager.getActivityInfo方法中未找到注册的Activity的异常
     com.malin.hook E/ActionBarView: Activity component name not found!
     android.content.pm.PackageManager$NameNotFoundException: ComponentInfo{com.malin.hook/com.malin.hook.TargetActivity}
     at android.app.ApplicationPackageManager.getActivityInfo(ApplicationPackageManager.java:241)
     at android.app.ApplicationPackageManager.getActivityLogo(ApplicationPackageManager.java:717)
     at com.android.internal.widget.ActionBarView.<init>(ActionBarView.java:196)
     at java.lang.reflect.Constructor.constructNative(Native Method)
     at java.lang.reflect.Constructor.newInstance(Constructor.java:417)
     at android.view.LayoutInflater.createView(LayoutInflater.java:594)
     at android.view.LayoutInflater.createViewFromTag(LayoutInflater.java:696)
     at android.view.LayoutInflater.rInflate(LayoutInflater.java:755)
     at android.view.LayoutInflater.rInflate(LayoutInflater.java:758)
     at android.view.LayoutInflater.rInflate(LayoutInflater.java:758)
     at android.view.LayoutInflater.inflate(LayoutInflater.java:492)
     at android.view.LayoutInflater.inflate(LayoutInflater.java:397)
     at android.view.LayoutInflater.inflate(LayoutInflater.java:353)
     at com.android.internal.policy.impl.PhoneWindow.generateLayout(PhoneWindow.java:2823)
     at com.android.internal.policy.impl.PhoneWindow.installDecor(PhoneWindow.java:2886)
     at com.android.internal.policy.impl.PhoneWindow.setContentView(PhoneWindow.java:282)
     at com.android.internal.policy.impl.PhoneWindow.setContentView(PhoneWindow.java:276)
     at android.app.Activity.setContentView(Activity.java:1915)
     at com.malin.hook.TargetActivity.onCreate(TargetActivity.java:23)
     at android.app.Activity.performCreate(Activity.java:5133)
     at android.app.Instrumentation.callActivityOnCreate(Instrumentation.java:1087)
     at android.app.ActivityThread.performLaunchActivity(ActivityThread.java:2175)
     at android.app.ActivityThread.handleLaunchActivity(ActivityThread.java:2261)
     at android.app.ActivityThread.access$600(ActivityThread.java:141)
     at android.app.ActivityThread$H.handleMessage(ActivityThread.java:1256)
     at android.os.Handler.dispatchMessage(Handler.java:99)
     at android.os.Looper.loop(Looper.java:137)
     at android.app.ActivityThread.main(ActivityThread.java:5103)
     at java.lang.reflect.Method.invokeNative(Native Method)
     at java.lang.reflect.Method.invoke(Method.java:525)
     at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:737)
     at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:553)
     at dalvik.system.NativeStart.main(Native Method)
     **/

    /**
     * 1.处理未注册的Activity为AppCompatActivity类或者子类的情况
     * 2.hook IPackageManager,处理android 4.3以下(<= 18)启动Activity,在ApplicationPackageManager.getActivityInfo方法中未找到注册的Activity的异常
     * 3.处理使用com.android.support:appcompat-v7:27.1.1会出现这个问题.使用28.0.0则不会
     * https://blog.csdn.net/gdutxiaoxu/article/details/81459910
     * <p>
     * http://weishu.me/2016/03/07/understand-plugin-framework-ams-pms-hook/
     *
     * @param context          context
     * @param subActivityClass 注册了的Activity的class对象
     */
    @SuppressLint({"DiscouragedPrivateApi", "PrivateApi"})
    static void hookPackageManager(Context context, Class<?> subActivityClass) {

        try {
            //1.获取ActivityThread的值
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            //public static ActivityThread currentActivityThread() {
            //        return sCurrentActivityThread;
            //    }
            Method currentActivityThreadMethod = activityThreadClass.getDeclaredMethod("currentActivityThread");
            currentActivityThreadMethod.setAccessible(true);
            Object activityThread = currentActivityThreadMethod.invoke(null);

            //2.获取ActivityThread里面原始的 sPackageManager
            //static IPackageManager sPackageManager;
            Field sPackageManagerField = activityThreadClass.getDeclaredField("sPackageManager");
            sPackageManagerField.setAccessible(true);
            Object sPackageManager = sPackageManagerField.get(activityThread);

            //3.准备好代理对象, 用来替换原始的对象
            Class<?> iPackageManagerClass = Class.forName("android.content.pm.IPackageManager");
            Object proxy = Proxy.newProxyInstance(
                    Thread.currentThread().getContextClassLoader(),
                    new Class<?>[]{iPackageManagerClass},
                    new PackageManagerProxyHandler(sPackageManager, getAppPackageName(context), subActivityClass.getName()));

            //4.替换掉ActivityThread里面的 sPackageManager 字段
            sPackageManagerField.set(activityThread, proxy);

            //5.替换 ApplicationPackageManager里面的 mPM对象
            PackageManager packageManager = context.getPackageManager();
            //PackageManager的实现类ApplicationPackageManager中的mPM
            //private final IPackageManager mPM;
            Field mPmField = packageManager.getClass().getDeclaredField("mPM");
            mPmField.setAccessible(true);
            mPmField.set(packageManager, proxy);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    private static class PackageManagerProxyHandler implements InvocationHandler {
        private String mSubActivityClassName;
        private Object mIPackageManagerObj;
        private String mAppPackageName;

        private PackageManagerProxyHandler(Object iPackageManagerObj, String appPackageName, String subActivityClassName) {
            this.mIPackageManagerObj = iPackageManagerObj;
            this.mSubActivityClassName = subActivityClassName;
            this.mAppPackageName = appPackageName;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
            //public android.content.pm.ActivityInfo getActivityInfo(android.content.ComponentName className, int flags, int userId)
            if ("getActivityInfo".equals(method.getName())) {
                int index = 0;
                for (int i = 0; i < args.length; i++) {
                    if (args[i] instanceof ComponentName) {
                        index = i;
                        break;
                    }
                }
                ComponentName componentName = new ComponentName(mAppPackageName, mSubActivityClassName);
                args[index] = componentName;
            }
            return method.invoke(mIPackageManagerObj, args);
        }
    }
}
