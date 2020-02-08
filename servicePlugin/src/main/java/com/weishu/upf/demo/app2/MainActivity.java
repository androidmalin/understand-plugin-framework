package com.weishu.upf.demo.app2;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.io.File;


public class MainActivity extends Activity {

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.d("TEST", "package added");
            Intent t = new Intent(context, ActionActivity.class);
            //402653184 = 0x18000000 = FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_MULTIPLE_TASK
            t.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            context.startActivity(t);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        findViewById(R.id.btn1).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                File f = new File("/sdcard/test.apk");
                Intent intent = new Intent("android.intent.action.VIEW");
                //268435456
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setDataAndType(Uri.fromFile(f), "application/vnd.android.package-archive");
                startActivity(intent);
            }
        });
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PACKAGE_ADDED");
        filter.addAction("android.intent.action.PACKAGE_REMOVED");
        filter.addDataScheme("package");
        registerReceiver(this.mReceiver, filter);
        startService(new Intent(this, MyService.class));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(this.mReceiver);
    }
}
