package com.weishu.upf.demo.app2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class Receiver2 extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        System.out.println("I am receiver 2");
    }
}