package com.weishu.upf.hook_classloader;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class PMSHook {

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

    /**
     * 获取包名
     */
    private static String getAppPackageName(Context context) {
        Context applicationContext = context.getApplicationContext();
        return applicationContext.getPackageName();
    }
}
