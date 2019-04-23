package com.example.janusandroidtalk.activity;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.janusandroidtalk.R;
import com.example.janusandroidtalk.signalingcontrol.JanusControl;
import com.example.janusandroidtalk.signalingcontrol.MyControlCallBack;
import com.example.janusandroidtalk.webrtc.ProxyVideoSink;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.EglBase;
import org.webrtc.MediaStream;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;

public class CallActivity extends AppCompatActivity implements MyControlCallBack {

    private LinearLayout layoutHangup;
    private LinearLayout layoutCancel;
    private LinearLayout layoutAccept;

    private LinearLayout layoutHead;

    private TextView tvName;
    private boolean isCall;
    private String name;
    private String jsep;
    private Chronometer tvTime;
    private TextView tvHangup;

    private SurfaceViewRenderer local_view;
    private SurfaceViewRenderer remote_view;
    private ProxyVideoSink localRender;
    private ProxyVideoSink remoteRender;

    private EglBase rootEglBase;
    private boolean isSwappedFeeds;

    private boolean isShowTips = true;

    private MediaPlayer mMediaPlayer;
    private CountDownTimer countDownTimer;

    protected void onCreate(Bundle savedInstanceState) {
        java.lang.System.setProperty("java.net.preferIPv6Addresses", "false");
        java.lang.System.setProperty("java.net.preferIPv4Stack", "true");
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_call);

        Bundle bundle = this.getIntent().getExtras();
        isCall = bundle.getBoolean("isCall",true);
        name = bundle.getString("name");
        jsep = bundle.getString("jsep");

        local_view =(SurfaceViewRenderer) findViewById(R.id.local_view_render);
        remote_view =(SurfaceViewRenderer) findViewById(R.id.remote_view_render);
        rootEglBase = EglBase.create();

        layoutHead = (LinearLayout) findViewById(R.id.audio_call_layout_head);
        layoutHangup = (LinearLayout) findViewById(R.id.audio_call_hangup);
        layoutCancel = (LinearLayout) findViewById(R.id.audio_call_cancel);
        layoutAccept = (LinearLayout) findViewById(R.id.audio_call_accept);

        tvName = (TextView) findViewById(R.id.audio_call_name);
        tvTime = (Chronometer) findViewById(R.id.audio_call_time);
        tvHangup = (TextView) findViewById(R.id.audio_call_cancel_text);

        //开启铃声
        mMediaPlayer=MediaPlayer.create(this, R.raw.call);
        mMediaPlayer.start();
        mMediaPlayer.setLooping(true);
        //开启一个倒计时线程，未接听关闭连接
        countDownTimer = new CountDownTimer(30000,1000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                JanusControl.sendHangup(CallActivity.this);
            }
        };
        countDownTimer.start();

        JanusControl.janusControlCreatePeerConnectionFactory(CallActivity.this,rootEglBase);

        //判断主呼叫和被呼叫
        if(isCall){
            layoutHangup.setVisibility(View.INVISIBLE);
            layoutCancel.setVisibility(View.VISIBLE);
            layoutAccept.setVisibility(View.INVISIBLE);
            tvName.setText(name);
            layoutHead.setVisibility(View.GONE);

            JanusControl.sendVideoCallCreateOffer(CallActivity.this,name);
        }else{
            layoutHangup.setVisibility(View.VISIBLE);
            layoutCancel.setVisibility(View.INVISIBLE);
            layoutAccept.setVisibility(View.VISIBLE);
            tvName.setText(name);
        }

        local_view = findViewById(R.id.local_view_render);
        remote_view = findViewById(R.id.remote_view_render);


        // 本地图像初始化
        local_view.init(rootEglBase.getEglBaseContext(), null);
        local_view.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        local_view.setZOrderMediaOverlay(true);
        local_view.setMirror(true);
        localRender = new ProxyVideoSink();
        //远端图像初始化
        remote_view.init(rootEglBase.getEglBaseContext(), null);
        remote_view.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_BALANCED);
        remote_view.setMirror(true);
        remoteRender = new ProxyVideoSink();
        setSwappedFeeds(true);

        local_view.setOnClickListener(v -> setSwappedFeeds(!isSwappedFeeds));

        layoutHangup.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                JanusControl.sendHangup(CallActivity.this);
            }
        });
        layoutCancel.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                JanusControl.sendHangup(CallActivity.this);
            }
        });
        layoutAccept.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                try{
                    layoutHead.setVisibility(View.GONE);
                    layoutHangup.setVisibility(View.INVISIBLE);
                    layoutCancel.setVisibility(View.VISIBLE);
                    layoutAccept.setVisibility(View.INVISIBLE);
                    JanusControl.sendVideoCallCreateAnswer(CallActivity.this,new JSONObject(jsep));
                }catch (Exception e){

                }

            }
        });

    }

    private void setSwappedFeeds(boolean isSwappedFeeds) {
        this.isSwappedFeeds = isSwappedFeeds;
        localRender.setTarget(isSwappedFeeds ? remote_view : local_view);
        remoteRender.setTarget(isSwappedFeeds ? local_view : remote_view);
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
                    Bundle bundle = new Bundle();
                    bundle.putString("name",msg.getJSONObject("result").getString("username"));
                    message3.setData(bundle);//mes利用Bundle传递数据
                    handler.sendMessage(message3);
                }
            }else if(msg.getString("videocall").equals("webRtcisok")){//webRtc链接成功

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSetLocalStream(MediaStream stream) {
        stream.videoTracks.get(0).setEnabled(true);
        stream.videoTracks.get(0).addSink(localRender);
    }

    @Override
    public void onAddRemoteStream(MediaStream stream) {
        setSwappedFeeds(false);
        stream.videoTracks.get(0).setEnabled(true);
        stream.videoTracks.get(0).addSink(remoteRender);
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
                                JanusControl.closeWebRtc();
                                disConnect();
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
                    countDownTimer.cancel();
                    mMediaPlayer.stop();
                    mMediaPlayer=MediaPlayer.create(CallActivity.this, R.raw.hangup);
                    mMediaPlayer.start();
                    mMediaPlayer.stop();

                    tvHangup.setText(R.string.audio_call_hangup);
                    tvTime.setVisibility(View.VISIBLE);
                    tvTime.setBase(SystemClock.elapsedRealtime());//计时器清零
                    int hour = (int) ((SystemClock.elapsedRealtime() - tvTime.getBase()) / 1000 / 60);
                    tvTime.setFormat("0"+String.valueOf(hour)+":%s");
                    tvTime.start();
                    break;
                case 3:
                    //hangup 挂断
                    if(msg.getData().getString("name").equals(name) && isShowTips){
                        mMediaPlayer.stop();
                        mMediaPlayer=MediaPlayer.create(CallActivity.this, R.raw.hangup);
                        mMediaPlayer.start();
                        mMediaPlayer.stop();
                        Toast.makeText(CallActivity.this,R.string.audio_call_hangup_remote_tip,Toast.LENGTH_SHORT).show();
                        Thread myThread1=new Thread(){
                            @Override
                            public void run() {
                                try{
                                    sleep(2000);
                                    JanusControl.closeWebRtc();
                                    disConnect();
                                    CallActivity.this.finish();
                                }catch (Exception e){
                                    e.printStackTrace();
                                }
                            }
                        };
                        myThread1.start();
                    }else{
                        mMediaPlayer.stop();
                        mMediaPlayer=MediaPlayer.create(CallActivity.this, R.raw.hangup);
                        mMediaPlayer.start();
                        mMediaPlayer.stop();
                        Toast.makeText(CallActivity.this,R.string.audio_call_hangup_tip,Toast.LENGTH_SHORT).show();
                        isShowTips = false;
                        Thread myThread1=new Thread(){
                            @Override
                            public void run() {
                                try{
                                    sleep(2000);
                                    JanusControl.closeWebRtc();
                                    disConnect();
                                    CallActivity.this.finish();
                                }catch (Exception e){
                                    e.printStackTrace();
                                }
                            }
                        };
                        myThread1.start();
                    }
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

    @Override
    protected void onDestroy() {
        disConnect();
        super.onDestroy();
    }

    private void disConnect() {
        if(mMediaPlayer != null){
            mMediaPlayer.release();
        }
        if(countDownTimer != null){
            countDownTimer.cancel();
        }

        if (localRender != null) {
            localRender.setTarget(null);
            localRender = null;
        }
        if (remoteRender != null) {
            remoteRender.setTarget(null);
            remoteRender = null;
        }

        if (local_view != null) {
            local_view.release();
            local_view = null;
        }
        if (remote_view != null) {
            remote_view.release();
            remote_view = null;
        }
    }

}
