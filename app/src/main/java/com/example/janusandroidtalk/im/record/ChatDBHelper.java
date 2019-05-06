package com.example.janusandroidtalk.im.record;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class ChatDBHelper extends SQLiteOpenHelper {

    private static final String TAG = "ChatDBHelper";
    private static final String DATABASE_NAME = "record.db";
    public static int DATABASE_VERSION = 1;

    public ChatDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "create table IF NOT EXISTS chat_record(_id INTEGER PRIMARY KEY AUTOINCREMENT,send_id smallint,send_name VARCHAR,receive_id smallint,"+
                     "msgid VARCHAR,send_time VARCHAR,chat_content TEXT,type int,status int,mediafilepath VARCHAR,duration smallint)";
        Log.d(TAG,TAG+"chatrecord create database ...");
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG,TAG+"chatrecord upgrade database ...");
    }
}