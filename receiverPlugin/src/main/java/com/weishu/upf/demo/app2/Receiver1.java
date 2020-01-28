package com.weishu.upf.demo.app2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class Receiver1 extends BroadcastReceiver {
    static final String ACTION = "com.weishu.upf.demo.app2.PLUGIN_ACTION";

    public void onReceive(Context context, Intent intent) {
        System.out.println("I am receiver 1 我是插件, 主程序收到请回答!");
        Toast.makeText(context, "我是插件, 主程序收到请回答!", Toast.LENGTH_SHORT).show();
        context.sendBroadcast(new Intent(ACTION));
    }
}