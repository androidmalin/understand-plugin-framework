package com.weishu.upf.receiver_management.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.util.Map;

/**
 * @author weishu
 * 16/4/7
 */
@SuppressLint("SetTextI18n")
public class MainActivity extends Activity {

    private static final String TAG = "XXYY";
    private static final String JAR_NAME = "receiverPlugin-debug.apk";
    // 发送广播到插件之后, 插件如果受到, 那么会回传一个ACTION 为这个值的广播;
    private static final String ACTION = "com.weishu.upf.demo.app2.PLUGIN_ACTION";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        initData();
    }

    private void initData() {
        Utils.extractAssets(this, JAR_NAME);
        File testPlugin = getFileStreamPath(JAR_NAME);
        try {
            ReceiverHelper.preLoadReceiver(this, testPlugin);
            Log.i(TAG, "hook success");
        } catch (Exception e) {
            throw new RuntimeException("receiver load failed", e);
        }
        // 注册插件收到我们发送的广播之后, 回传的广播
        registerReceiver(mReceiver, new IntentFilter(ACTION));
    }

    private void test() {
        Utils.extractAssets(this, "app-debug.apk");
        File testPlugin = getFileStreamPath("app-debug.apk");
        try {
            ApplicationInfo applicationInfo = ApkUtils.getApplicationInfo(testPlugin, this);
            String name = applicationInfo.packageName;
            System.out.println("packageName:" + name);

            Map<ComponentName, ActivityInfo> map = ApkUtils.getActivityInfos(testPlugin, this);
            for (Map.Entry<ComponentName, ActivityInfo> entry : map.entrySet()) {
                System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initView() {
        Button button = new Button(this);
        setContentView(button);
        button.setText("send broadcast to plugin");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //initData();
                //test();
                sendBroadcast(new Intent("com.weishu.upf.demo.app2.Receiver1"));
                sendBroadcast(new Intent("com.weishu.upf.demo.app2.Receiver2"));
            }
        });
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive 插件插件,我是主程序,握手完成!");
            Toast.makeText(context, "插件插件,我是主程序,握手完成!", Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }
}
