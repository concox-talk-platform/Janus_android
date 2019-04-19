package com.example.janusandroidtalk;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.example.janusandroidtalk.tools.AppFrontBackHelper;

public class MyApplication extends Application {
    private static MyApplication instance;
    private static Context context;
    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor sharedPreferencesEditor;

    public static MyApplication getInstance() {
        return instance;
    }

    public static Context getContext() {
        return context;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        context = getApplicationContext();

        sharedPreferences = getSharedPreferences("setting", Context.MODE_PRIVATE);
        sharedPreferencesEditor = sharedPreferences.edit();

    }

    /**
     * 设置 token
     * */
    public static void setLoginState(String token){
        sharedPreferencesEditor.putString("loginState",token);
        sharedPreferencesEditor.commit();
    }
    /**
     * 获取 token
     * */
    public static String getLoginState(){
        return sharedPreferences.getString("loginState","");
    }

    /**
     * 设置 进入默认的群组ID
     * */
    public static void setDefaultGroupId(int groupId){
        sharedPreferencesEditor.putInt("defaultGroupId",groupId);
        sharedPreferencesEditor.commit();
    }
    /**
     * 获取 获取默认进入的群组ID
     * */
    public static int getDefaultGroupId(){
        return sharedPreferences.getInt("defaultGroupId",0);
    }

    public static void setUserId(int userId){
        sharedPreferencesEditor.putInt("userId",userId);
        sharedPreferencesEditor.commit();
    }
    public static int getUserId(){
        return sharedPreferences.getInt("userId",0);
    }

    public static void setUserName(String userName){
        sharedPreferencesEditor.putString("userName",userName);
        sharedPreferencesEditor.commit();
    }
    public static String getUserName(){
        return sharedPreferences.getString("userName","");
    }

    public static void setPassword(String userName){
        sharedPreferencesEditor.putString("password",userName);
        sharedPreferencesEditor.commit();
    }
    public static String getPassword(){
        return sharedPreferences.getString("password","");
    }

    /**
     * 清楚所有的sharedPreferences数据
     * */
    public static void clearMyData(){
        sharedPreferencesEditor.clear();
    }

}
