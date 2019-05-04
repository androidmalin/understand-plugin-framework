package com.weishu.upf.ams_pms_hook.app;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

/**
 * @author weishu
 * @date 16/3/7
 */
public class MainActivity extends Activity implements OnClickListener {

    private static final String TAG = "NewAPP";

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        try {
            setContentView(R.layout.main);
        } catch (Throwable throwable) {
            Log.e(TAG, throwable.getMessage(), throwable);
        }
        try {
            findViewById(R.id.btn1).setOnClickListener(this);
            findViewById(R.id.btn2).setOnClickListener(this);
        } catch (Throwable throwable) {
            Log.e(TAG, throwable.getMessage(), throwable);
        }
    }

    // 这个方法比onCreate调用早; 在这里Hook比较好.
    @Override
    protected void attachBaseContext(Context newBase) {
        HookHelper.hookActivityManager();
        HookHelper.hookPackageManager(newBase);
        super.attachBaseContext(newBase);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn1:
                Toast.makeText(getApplicationContext(), "one", Toast.LENGTH_LONG).show();
                break;
            case R.id.btn2:
                //getPackageManager().getInstalledApplications(0);
                Toast.makeText(getApplicationContext(), "two", Toast.LENGTH_LONG).show();
                break;
        }
    }
}
