package com.example.weishu.testcontentprovider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.Arrays;

/**
 * @author weishu
 * 16/7/8.
 */
public class TestContentProvider extends ContentProvider {

    private static final String TAG = "TestXXII";

    public static final String AUTHORITY = "com.example.weishu.testcontentprovider.TestContentProvider";

    public static final Uri URI = Uri.parse("content://" + AUTHORITY);

    public static final String NAME = "name";

    private static final String TABLE_NAME = "person";

    private TestDataBase mDb;

    @Override
    public boolean onCreate() {
        Log.d(TAG, "TestContentProvider#onCreate()");
        mDb = new TestDataBase(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        SQLiteDatabase db = mDb.getReadableDatabase();
        qb.setTables(TABLE_NAME);
        Cursor c = qb.query(db, projection, selection, null, null, null, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        Log.d(TAG, "TestContentProvider#query() uri==> " + uri.getPath());
        Log.d(TAG, "TestContentProvider#query() projection==> " + Arrays.toString(projection));
        Log.d(TAG, "TestContentProvider#query() selection==> " + selection);
        Log.d(TAG, "TestContentProvider#query() selectionArgs==> " + Arrays.toString(selectionArgs));
        Log.d(TAG, "TestContentProvider#query() sortOrder==> " + sortOrder);
        return c;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        Log.d(TAG, "TestContentProvider#getType() uri:" + uri);
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        Log.d(TAG, "TestContentProvider#insert() uri==> " + uri);
        Log.d(TAG, "TestContentProvider#insert() values==> " + values.toString());
        SQLiteDatabase sqlDB = mDb.getWritableDatabase();
        long rowId = sqlDB.insert(TABLE_NAME, "", values);
        if (rowId > 0) {
            Uri rowUri = ContentUris.appendId(URI.buildUpon(), rowId).build();
            getContext().getContentResolver().notifyChange(rowUri, null);
            return rowUri;
        }
        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        Log.d(TAG, "TestContentProvider#delete() uri==> " + uri);
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        Log.d(TAG, "TestContentProvider#update() uri==> " + uri);
        return 0;
    }

    private static class TestDataBase extends SQLiteOpenHelper {

        private static int VERSION = 1;
        private static final String DB_NAME = "persons.db";

        TestDataBase(Context context) {
            super(context, DB_NAME, null, VERSION);
            Log.d(TAG, "TestContentProvider$TestDataBase#TestDataBase()");
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            String createSql = "Create table " + TABLE_NAME + "( _id INTEGER PRIMARY KEY AUTOINCREMENT, " + NAME + ");";
            Log.d(TAG, "TestContentProvider$TestDataBase#onCreate(): createSql: " + createSql);
            db.execSQL(createSql);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.d(TAG, "TestContentProvider$TestDataBase#onUpgrade() oldVersion:" + oldVersion + ", newVersion:" + newVersion);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }
    }
}
