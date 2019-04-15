package com.example.janusandroidtalk.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import com.example.janusandroidtalk.MainActivity;
import com.example.janusandroidtalk.MyApplication;
import com.example.janusandroidtalk.R;

public class LaunchActivity extends AppCompatActivity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //getSupportActionBar().hide();
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
