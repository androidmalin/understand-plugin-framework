package com.example.weishu.contentprovider_management;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

/**
 * content://
 *
 * @author weishu
 * 16/7/11.
 */
public class StubContentProvider extends ContentProvider {

    private static final String TAG = "StubContentProvider";

    public static final String AUTHORITY = "com.example.weishu.contentprovider_management.StubContentProvider";

    @Override
    public boolean onCreate() {
        Log.e(TAG, "StubContentProvider#onCreate()");
        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Log.e(TAG, "StubContentProvider#query()");
        //noinspection ConstantConditions
        return getContext().getContentResolver().query(getRealUri(uri), projection, selection, selectionArgs, sortOrder);
    }

    @Override
    public String getType(@NonNull Uri uri) {
        Log.e(TAG, "StubContentProvider#getType()");
        return null;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        Log.e(TAG, "StubContentProvider#insert()");
        //noinspection ConstantConditions
        return getContext().getContentResolver().insert(getRealUri(uri), values);
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        Log.e(TAG, "StubContentProvider#delete()");
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        Log.e(TAG, "StubContentProvider#update()");
        return 0;
    }

    /**
     * 为了使得插件的ContentProvider提供给外部使用，我们需要一个StubProvider做中转；
     * 如果外部程序需要使用插件系统中插件的ContentProvider，不能直接查询原来的那个uri
     * 我们对uri做一些手脚，使得插件系统能识别这个uri；
     * <p>
     * 这里的处理方式如下：
     * <p>
     * 原始查询插件的URI应该为：
     * content://plugin_auth/path/query
     * <p>
     * 如果需要查询插件，需要修改为：
     * <p>
     * content://stub_auth/plugin_auth/path/query
     * <p>
     * 也就是，我们把插件ContentProvider的信息放在URI的path中保存起来；
     * 然后在StubProvider中做分发。
     * <p>
     * 当然，也可以使用QueryParamerter,比如：
     * content://plugin_auth/path/query/ ->  content://stub_auth/path/query?plugin=plugin_auth
     *
     * @param raw 外部查询我们使用的URI
     * @return 插件真正的URI
     */
    private Uri getRealUri(Uri raw) {
        Log.e(TAG, "StubContentProvider#getRealUri()");
        String rawAuth = raw.getAuthority();
        if (!AUTHORITY.equals(rawAuth)) {
            Log.w(TAG, "rawAuth:" + rawAuth);
        }

        String uriString = raw.toString();
        uriString = uriString.replaceAll(rawAuth + '/', "");
        Uri newUri = Uri.parse(uriString);
        Log.i(TAG, "realUri:" + newUri);
        return newUri;
    }

}
