package com.weishu.upf.demo.app2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class Receiver1 extends BroadcastReceiver {

    private static final String ACTION = "com.weishu.upf.demo.app2.PLUGIN_ACTION";

    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "我是插件, 主程序收到请回答!", Toast.LENGTH_SHORT).show();
        context.sendBroadcast(new Intent(ACTION));
    }
}
