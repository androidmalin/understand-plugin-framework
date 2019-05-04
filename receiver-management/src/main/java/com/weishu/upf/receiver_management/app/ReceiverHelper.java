package com.weishu.upf.receiver_management.app;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author weishu
 * @date 16/4/7
 * >=5.0
 */
@SuppressWarnings("JavaReflectionMemberAccess")
@SuppressLint({"PrivateApi", "unchecked"})
public final class ReceiverHelper {

    private static final String TAG = "ReceiverHelper";

    public static Map<ActivityInfo, List<? extends IntentFilter>> sCache = new HashMap<>();

    /**
     * 解析Apk文件中的 <receiver>, 并存储起来
     */
    private static void parserReceivers(File apkFile) throws Exception {

        //1.获取PackageParser的Class对象
        //package android.content.pm
        //public class PackageParser
        Class<?> packageParserClass = Class.forName("android.content.pm.PackageParser");

        //2.获取parsePackage()方法的Field
        //public Package parsePackage(File packageFile, int flags){}//api-28
        Method parsePackageMethod = packageParserClass.getDeclaredMethod("parsePackage", File.class, int.class);

        //3.生成PackageParser对象实例
        Object packageParser = packageParserClass.newInstance();

        //4.调用parsePackage获取到apk对象对应的Package对象
        Object packageObj = parsePackageMethod.invoke(packageParser, apkFile, PackageManager.GET_RECEIVERS);

        //5.获取Package中的receivers字段Field
        // 读取Package对象里面的receivers字段,注意这是一个 List<Activity> (没错,底层把<receiver>当作<activity>处理)
        // 接下来要做的就是根据这个List<Activity> 获取到Receiver对应的 ActivityInfo (依然是把receiver信息用activity处理了)
        //public final ArrayList<Activity> receivers = new ArrayList<Activity>(0);
        Field receiversField = packageObj.getClass().getDeclaredField("receivers");

        //6.获取Package实例中receivers字段的值,为ArrayList<Activity> receivers
        List receivers = (List) receiversField.get(packageObj);

        // 调用generateActivityInfo 方法, 把PackageParser.Activity 转换成

        //7.获取PackageParser类的内部类Activity的Class对象
        // public final static class Activity
        Class<?> packageParser$ActivityClass = Class.forName("android.content.pm.PackageParser$Activity");

        //8.获取PackageUserState的Class对象
        Class<?> packageUserStateClass = Class.forName("android.content.pm.PackageUserState");

        //9.获取UserHandle的Class对象
        Class<?> userHandler = Class.forName("android.os.UserHandle");

        //10.获取UserHandle中的getCallingUserId()方法
        //public static @UserIdInt int getCallingUserId()
        Method getCallingUserIdMethod = userHandler.getDeclaredMethod("getCallingUserId");

        //11.获取getCallingUserId()方法(静态方法)的返回值userId
        int userId = (Integer) getCallingUserIdMethod.invoke(null);

        //12.生成PackageUserState的实例对象
        Object defaultUserState = packageUserStateClass.newInstance();

        //13.获取PackageParser类中内部类Component
        //public static abstract class Component<II extends IntentInfo>
        Class<?> componentClass = Class.forName("android.content.pm.PackageParser$Component");

        //14.获取 Component类的intents属性的Field
        //public final ArrayList<II> intents;
        Field intentsField = componentClass.getDeclaredField("intents");

        //15.调用 public static final ActivityInfo android.content.pm.PackageParser#generateActivityInfo(
        // android.content.pm.PackageParser$Activity activity,
        // int flags,
        // android.content.pm.PackageUserState state,
        // int userId)
        Method generateReceiverInfo = packageParserClass.getDeclaredMethod("generateActivityInfo",
                packageParser$ActivityClass, int.class, packageUserStateClass, int.class);

        //16.解析出 receiver以及对应的 intentFilter
        //receivers -->ArrayList<Activity> receivers
        for (Object receiver : receivers) {
            ActivityInfo info = (ActivityInfo) generateReceiverInfo.invoke(packageParser, receiver, 0, defaultUserState, userId);

            //17.PackageParser$Activity中获取intents字段的值,既public final ArrayList<II> intents;
            //List<? extends IntentFilter>
            List<? extends IntentFilter> filters = (List<? extends IntentFilter>) intentsField.get(receiver);
            sCache.put(info, filters);
        }

    }

    public static void preLoadReceiver(Context context, File apk) throws Exception {
        parserReceivers(apk);

        ClassLoader cl = null;
        for (ActivityInfo activityInfo : ReceiverHelper.sCache.keySet()) {
            Log.i(TAG, "preload receiver:" + activityInfo.name);
            List<? extends IntentFilter> intentFilters = ReceiverHelper.sCache.get(activityInfo);
            if (cl == null) {
                cl = CustomClassLoader.getPluginClassLoader(apk, activityInfo.packageName);
            }

            if (intentFilters == null) continue;
            // 把解析出来的每一个静态Receiver都注册为动态的
            for (IntentFilter intentFilter : intentFilters) {
                BroadcastReceiver receiver = (BroadcastReceiver) cl.loadClass(activityInfo.name).newInstance();
                context.registerReceiver(receiver, intentFilter);
            }
        }
    }
}
