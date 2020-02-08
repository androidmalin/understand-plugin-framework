package com.weishu.upf.demo.app2;

import android.app.Activity;
import android.os.Bundle;

public class ActionActivity extends Activity {

    private static final String ACTION = "com.weishu.upf.demo.app2.ActionActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }
}
