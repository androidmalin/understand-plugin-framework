package com.weishu.upf.service_management.app;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * @author weishu
 * 16/5/10
 */
public class ProxyService extends Service {

    private static final String TAG = "ProxyService";

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate() called");
        super.onCreate();
    }

    /**
     * Hook ActivityManagerNative之后，所有的插件Service的启动都被重定向了到了我们注册的ProxyService，
     * 这样可以保证我们的插件Service有一个真正的Service组件作为宿主；
     * 但是要执行特定插件Service的任务，我们必须把这个任务分发到真正要启动的Service上去；
     * 以onStart为例，在启动ProxyService之后，会收到ProxyService的onStart回调，
     * 我们可以在这个方法里面把具体的任务交给原始要启动的插件Service组件：
     */
    @Override
    public void onStart(Intent intent, int startId) {
        Log.d(TAG, "onStart() called with " + "intent = [" + intent + "], startId = [" + startId + "]");

        // 分发Service
        ServiceManager.getInstance().onStart(intent, startId);
        super.onStart(intent, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: 16/5/11 bindService实现
        //如果用bindService方式绑定插件Service，可以调用对应Service对应的onBind方法，
        // 获取onBind方法返回的Binder对象，然后通过ServiceConnection对象进行回调统计；
        // unBindService的实现同理。

        return null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy() called");
        super.onDestroy();
    }
}
