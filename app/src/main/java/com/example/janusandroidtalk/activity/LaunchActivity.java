package com.example.janusandroidtalk.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.view.WindowManager;

import com.example.janusandroidtalk.MainActivity;
import com.example.janusandroidtalk.MyApplication;
import com.example.janusandroidtalk.R;

public class LaunchActivity extends AppCompatActivity {

    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);
        Thread myThread=new Thread(){
            @Override
            public void run() {
                try{
                    sleep(1000);
                    Intent intent = null;
                    if(MyApplication.getLoginState().equals("login")){
                        intent = new Intent(getApplicationContext(),MainActivity.class);//启动MainActivity
                    }else{
                        intent = new Intent(getApplicationContext(),LoginActivity.class);
                    }
                    startActivity(intent);
                    finish();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        };
        myThread.start();
    }
}
