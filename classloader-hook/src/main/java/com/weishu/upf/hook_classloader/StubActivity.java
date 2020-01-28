package com.weishu.upf.hook_classloader;

import android.app.Activity;
import android.os.Bundle;

/**
 * @author weishu
 * @date 16/3/29
 */
public class StubActivity extends Activity {
    // dummy


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("StubActivity");
    }
}
