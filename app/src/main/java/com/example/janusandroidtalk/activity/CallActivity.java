package com.example.janusandroidtalk.activity;

import android.opengl.EGLContext;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.janusandroidtalk.R;
import com.example.janusandroidtalk.signalingcontrol.JanusControl;
import com.example.janusandroidtalk.signalingcontrol.MyControlCallBack;
import com.example.janusandroidtalk.webrtctest.VideoCallActivity;
import com.example.janusandroidtalk.webrtctest.VideoCallTest;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;

public class CallActivity extends AppCompatActivity implements MyControlCallBack {

    private LinearLayout layoutHangup;
    private LinearLayout layoutCancel;
    private LinearLayout layoutAccept;

    private LinearLayout layoutGLView;
    private LinearLayout layoutHead;

    private TextView textView;
    private boolean isCall;
    private String name;
    private String jsep;

    private GLSurfaceView glSurfaceView;
    private VideoRenderer.Callbacks localRender;
    private VideoRenderer.Callbacks remoteRender;

    private class MyCreateOffer implements Runnable {

        public void run() {
            init();
        }

        private void init() {
            try {
                JanusControl.sendVideoCallCreateOffer(CallActivity.this,name,localRender,remoteRender);
            } catch (Exception ex) {
                Log.e("computician.janusclient", ex.getMessage());
            }
        }
    }

    private class MyCreateAnswer implements Runnable {

        public void run() {
            init();
        }

        private void init() {
            try {
                JSONObject jsonObject = new JSONObject(jsep);
                JanusControl.sendVideoCallCreateAnswer(CallActivity.this,jsonObject,localRender,remoteRender);
            } catch (Exception ex) {
                Log.e("computician.janusclient", ex.getMessage());
            }
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_audio_call);

        Bundle bundle = this.getIntent().getExtras();
        isCall = bundle.getBoolean("isCall",true);
        name = bundle.getString("name");
        jsep = bundle.getString("jsep");


        layoutGLView = (LinearLayout) findViewById(R.id.audio_call_layout_gl_view);
        layoutHead = (LinearLayout) findViewById(R.id.audio_call_layout_head);
        layoutHangup = (LinearLayout) findViewById(R.id.audio_call_hangup);
        layoutCancel = (LinearLayout) findViewById(R.id.audio_call_cancel);
        layoutAccept = (LinearLayout) findViewById(R.id.audio_call_accept);

        textView = (TextView) findViewById(R.id.audio_call_name);

        glSurfaceView = (GLSurfaceView) findViewById(R.id.audio_call_gl_view);
        glSurfaceView.setPreserveEGLContextOnPause(true);
        glSurfaceView.setKeepScreenOn(true);


        //判断主呼叫和被呼叫
        if(isCall){
            layoutHangup.setVisibility(View.INVISIBLE);
            layoutCancel.setVisibility(View.VISIBLE);
            layoutAccept.setVisibility(View.INVISIBLE);
            textView.setText(name);

            VideoRendererGui.setView(glSurfaceView, new MyCreateOffer());
            localRender = VideoRendererGui.create(0, 0, 25, 25, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false);
            remoteRender = VideoRendererGui.create(72, 0, 25, 25, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true);
            layoutGLView.setVisibility(View.INVISIBLE);
        }else{
            layoutHangup.setVisibility(View.VISIBLE);
            layoutCancel.setVisibility(View.INVISIBLE);
            layoutAccept.setVisibility(View.VISIBLE);
            textView.setText(name);

            VideoRendererGui.setView(glSurfaceView, new MyCreateAnswer());
            localRender = VideoRendererGui.create(2, 2, 30, 30, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false);
            remoteRender = VideoRendererGui.create(68, 2, 30, 30, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true);
        }

        layoutHangup.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                JanusControl.sendHangup(CallActivity.this);
                JanusControl.closeWebRtc();
                Toast.makeText(CallActivity.this,R.string.audio_call_hangup_tip,Toast.LENGTH_SHORT).show();
                Thread myThread1=new Thread(){
                    @Override
                    public void run() {
                        try{
                            sleep(2000);
                            CallActivity.this.finish();
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                };
                myThread1.start();
            }
        });
        layoutCancel.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                JanusControl.sendHangup(CallActivity.this);
                JanusControl.closeWebRtc();
                Toast.makeText(CallActivity.this,R.string.audio_call_hangup_tip,Toast.LENGTH_SHORT).show();
                Thread myThread1=new Thread(){
                    @Override
                    public void run() {
                        try{
                            sleep(2000);
                            CallActivity.this.finish();
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                };
                myThread1.start();
            }
        });
        layoutAccept.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {

            }
        });

    }

    //回调函数
    @Override
    public void janusServer(Boolean isOk) {

    }
    @Override
    public void showMessage(JSONObject msg,JSONObject jsepLocal) {
        try {
            if (msg.getString("videocall").equals("videoCallIsOk")) {

            }else if(msg.getString("videocall").equals("event")){// 信令成功
                if(msg.has("error")){
                    Message message = new Message();
                    message.what = 0;
                    Bundle bundle = new Bundle();
                    bundle.putString("error",msg.getString("error"));
                    message.setData(bundle);//mes利用Bundle传递数据
                    handler.sendMessage(message);
                }else if(msg.getJSONObject("result").getString("event").equals("registered")){

                }else if(msg.getJSONObject("result").getString("event").equals("calling")){
                    Message message1 = new Message();
                    message1.what = 1;
                    handler.sendMessage(message1);
                }else if(msg.getJSONObject("result").getString("event").equals("incomingcall")){

                }else if(msg.getJSONObject("result").getString("event").equals("accepted")){
                    Message message2 = new Message();
                    message2.what = 2;
                    handler.sendMessage(message2);
                }else if(msg.getJSONObject("result").getString("event").equals("set")){

                }else if(msg.getJSONObject("result").getString("event").equals("update")){

                }else if(msg.getJSONObject("result").getString("event").equals("hangup")){
                    Message message3 = new Message();
                    message3.what = 3;
                    handler.sendMessage(message3);
                }
            }else if(msg.getString("videocall").equals("webRtcisok")){//webRtc链接成功

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private Handler handler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case 0:
                    Toast.makeText(CallActivity.this,msg.getData().getString("error"),Toast.LENGTH_SHORT).show();
                    Thread myThread=new Thread(){
                        @Override
                        public void run() {
                            try{
                                sleep(2000);
                                CallActivity.this.finish();
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    };
                    myThread.start();
                    break;
                case 1:
                    //calling 播放声音

                    break;
                case 2:
                    //accepted 对方接受 关闭声音，
                    layoutHead.setVisibility(View.GONE);
                    layoutGLView.setVisibility(View.VISIBLE);
                    layoutHangup.setVisibility(View.INVISIBLE);
                    layoutCancel.setVisibility(View.VISIBLE);
                    layoutAccept.setVisibility(View.INVISIBLE);
                    break;
                case 3:
                    //hangup 对方挂断
                    JanusControl.closeWebRtc();
                    Toast.makeText(CallActivity.this,R.string.audio_call_hangup_remote_tip,Toast.LENGTH_SHORT).show();
                    Thread myThread1=new Thread(){
                        @Override
                        public void run() {
                            try{
                                sleep(2000);
                                CallActivity.this.finish();
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    };
                    myThread1.start();
                    break;
            }
        };
    };


    // 物理返回键
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

}
