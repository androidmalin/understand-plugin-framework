package com.weishu.upf.receiver_management.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;

/**
 * @author weishu
 * @date 16/4/7
 */
@SuppressLint("SetTextI18n")
public class MainActivity extends Activity {

    private static final String JAR_NAME = "test.jar";
    // 发送广播到插件之后, 插件如果受到, 那么会回传一个ACTION 为这个值的广播;
    private static final String ACTION = "com.weishu.upf.demo.app2.PLUGIN_ACTION";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        //initData();
    }

    private void initData() {
        Utils.extractAssets(this, JAR_NAME);
        File testPlugin = getFileStreamPath(JAR_NAME);
        try {
            ReceiverHelper.preLoadReceiver(this, testPlugin);
            Log.i(getClass().getSimpleName(), "hook success");
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

            System.out.println(name);
            ApkUtils.getActivityInfos(testPlugin, this);
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
                initData();
                //test();
                //Toast.makeText(getApplicationContext(), "插件插件!收到请回答!!", Toast.LENGTH_SHORT).show();
                //sendBroadcast(new Intent("com.weishu.upf.demo.app2.Receiver1"));
            }
        });
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(context, "插件插件,我是主程序,握手完成!", Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }
}
