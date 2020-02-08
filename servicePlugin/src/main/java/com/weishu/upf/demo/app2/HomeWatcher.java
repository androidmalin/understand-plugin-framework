package com.weishu.upf.demo.app2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class HomeWatcher {

    private static final String TAG = "hg";
    private Context mContext;
    private IntentFilter mFilter = new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS");
    private OnHomePressedListener mListener;
    private InnerReceiver mInnerReceiver;

    public interface OnHomePressedListener {
        void onHomeLongPressed();

        void onHomePressed();
    }

    public HomeWatcher(Context context) {
        this.mContext = context;
    }

    public void setOnHomePressedListener(OnHomePressedListener listener) {
        this.mListener = listener;
        this.mInnerReceiver = new InnerReceiver();
    }

    public void startWatch() {
        if (this.mInnerReceiver != null) {
            this.mContext.registerReceiver(this.mInnerReceiver, this.mFilter);
        }
    }

    public void stopWatch() {
        if (this.mInnerReceiver != null) {
            this.mContext.unregisterReceiver(this.mInnerReceiver);
        }
    }

    private class InnerReceiver extends BroadcastReceiver {
        final String SYSTEM_DIALOG_REASON_GLOBAL_ACTIONS = "globalactions";
        final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";
        final String SYSTEM_DIALOG_REASON_KEY = "reason";
        final String SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps";

        private InnerReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            String reason;
            String action = intent.getAction();
            if (action.equals("android.intent.action.CLOSE_SYSTEM_DIALOGS") && (reason = intent.getStringExtra("reason")) != null) {
                Log.e(HomeWatcher.TAG, "action:" + action + ",reason:" + reason);
                if (HomeWatcher.this.mListener == null) {
                    return;
                }
                if (reason.equals("homekey")) {
                    HomeWatcher.this.mListener.onHomePressed();
                } else if (reason.equals("recentapps")) {
                    HomeWatcher.this.mListener.onHomeLongPressed();
                }
            }
        }
    }
}
