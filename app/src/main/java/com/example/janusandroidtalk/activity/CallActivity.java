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

import com.example.janusandroidtalk.MyApplication;
import com.example.janusandroidtalk.R;
import com.example.janusandroidtalk.floatwindow.FloatActionController;
import com.example.janusandroidtalk.fragment.FragmentGroup;
import com.example.janusandroidtalk.fragment.FragmentMine;
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
    private boolean isVideo;
    private String name;
    private int remoteId;
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

    private boolean hangupOrcancel = false;//for usercalled
    private boolean connectionIsOk = false;//通话连接建立成功
    private boolean isMyHangup = false;//通话连接建立成功之后，主动挂断

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

        //隐藏悬浮窗
        FloatActionController.getInstance().hide();

        Bundle bundle = this.getIntent().getExtras();
        isCall = bundle.getBoolean("isCall",true);
        isVideo = bundle.getBoolean("isVideo",true);
        name = bundle.getString("name");
        remoteId = bundle.getInt("remoteId");

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
        countDownTimer = new CountDownTimer(60000,1000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                if(isCall){
                    hangupOrcancel= true;
                    JanusControl.sendUserCall(CallActivity.this,remoteId,2,true,3);
                }else{
                    hangupOrcancel = true;
                    JanusControl.sendUserCall(CallActivity.this,remoteId,1,true,1);
                }
            }
        };
        countDownTimer.start();

        //判断主呼叫和被呼叫
        if(isCall){
            layoutHangup.setVisibility(View.INVISIBLE);
            layoutCancel.setVisibility(View.VISIBLE);
            layoutAccept.setVisibility(View.INVISIBLE);
            if(isVideo){
                layoutHead.setVisibility(View.GONE);
                local_view.setVisibility(View.VISIBLE);
                remote_view.setVisibility(View.VISIBLE);
            }else{
                layoutHead.setVisibility(View.VISIBLE);
                local_view.setVisibility(View.GONE);
                remote_view.setVisibility(View.GONE);
            }
            tvName.setText(name);
            //发送 user_call信令 先协商是否，成功之后
            JanusControl.sendUserCall(CallActivity.this,remoteId,0,true,0);
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
                //拒绝
                hangupOrcancel = true;
                JanusControl.sendUserCall(CallActivity.this,remoteId,1,true,1);
            }
        });
        layoutCancel.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                if(connectionIsOk){
                    isMyHangup = true;
                    Toast.makeText(CallActivity.this,R.string.audio_call_hangup_tip,Toast.LENGTH_SHORT).show();
                    reJoinRoom();
                }else {
                    hangupOrcancel = true;
                    JanusControl.sendUserCall(CallActivity.this, remoteId, 2, true, 3);
                }
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
                    hangupOrcancel = false;
                    JanusControl.janusControlPocRoomDetach(CallActivity.this);
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
    public void janusServer(int code,String msg) {
        switch (code){
            case 101:
                // create 成功
                Message message101 = new Message();
                message101.what = 101;
                handler.sendMessage(message101);
                break;
            case 102:
                //user_called 成功 isAccept 1拒绝，2，接受，3取消
                if(hangupOrcancel){
                    //取消，直接挂断
                    Message message102 = new Message();
                    message102.what = 102;
                    handler.sendMessage(message102);
                }
                break;
            case 103:
                //user_call 成功
                if(msg.equals("1")){
                    //对方拒绝，
                    Message message103 = new Message();
                    message103.what = 103;
                    handler.sendMessage(message103);
                }else if(msg.equals("2")){
                    //对方接受，使用vc插件，注册，发起vc。
                    JanusControl.sendAttachVideoCallPlugin(CallActivity.this);
                }
                break;
            case 104:
                //pocroom onDetached 被呼叫方
                JanusControl.sendAttachVideoCallPlugin(CallActivity.this);
                break;
            case 100:
                // 失败
                Message message100 = new Message();
                message100.what = 100;
                Bundle bundle = new Bundle();
                bundle.putString("error",msg);
                message100.setData(bundle);//mes利用Bundle传递数据
                handler.sendMessage(message100);
                break;
        }
    }
    @Override
    public void showMessage(JSONObject msg,JSONObject jsepLocal) {
        try {
            if(msg.has("pocroom")){
                if(msg.getString("pocroom").equals("audiobridgeisok")){
                    JanusControl.janusControlCreatePeerConnectionFactory(CallActivity.this);
                    if(MyApplication.getDefaultGroupId()!=0){//不为空才直接加入房间
                        JanusControl.sendPocRoomJoinRoom(CallActivity.this,MyApplication.getDefaultGroupId());
                    }
                }else if(msg.getString("pocroom").equals("joined")){//加入房间成功，开始创建offer,进行webRtc链接
                    if(msg.has("id") && msg.getInt("id") == MyApplication.getUserId() ){
                        JanusControl.sendPocRoomCreateOffer(CallActivity.this);
                    }
                }else if(msg.getString("pocroom").equals("configured")){
                    Message message4 = new Message();
                    message4.what = 4;
                    handler.sendMessage(message4);
                }
            }
            if(msg.has("videocall")){
                if (msg.getString("videocall").equals("videoCallIsOk")) {
                    JanusControl.sendRegister();
                }else if(msg.getString("videocall").equals("event")){// 信令成功
                    if(msg.has("error")){
                        Message message0 = new Message();
                        message0.what = 0;
                        Bundle bundle = new Bundle();
                        bundle.putString("error",msg.getString("error"));
                        message0.setData(bundle);//mes利用Bundle传递数据
                        handler.sendMessage(message0);
                    }else if(msg.getJSONObject("result").getString("event").equals("registered")){
                        if(isCall){//主呼叫方注册成功，发起呼叫
                            if(isVideo){
                                JanusControl.janusControlCreatePeerConnectionFactory(CallActivity.this,rootEglBase);//创建音频，视频
                                JanusControl.sendVideoCallCreateOffer(CallActivity.this,name,isVideo);
                            }else{
                                JanusControl.janusControlCreatePeerConnectionFactory(CallActivity.this);//创建音频
                                JanusControl.sendVideoCallCreateOffer(CallActivity.this,name,isVideo);
                            }
                            //JanusControl.sendPocRoomVideoCallCreateOffer(CallActivity.this,name,isVideo,rootEglBase,CallActivity.this);
                        }else{//被呼叫方注册成功，发送userCall，accept=2给主叫方
                            if(isVideo) {
                                JanusControl.sendUserCall(CallActivity.this, remoteId, 1, true, 2);
                            }else{
                                JanusControl.sendUserCall(CallActivity.this, remoteId, 1, false, 2);
                            }
                        }
                    }else if(msg.getJSONObject("result").getString("event").equals("calling")){
                        Message message1 = new Message();
                        message1.what = 1;
                        handler.sendMessage(message1);
                    }else if(msg.getJSONObject("result").getString("event").equals("incomingcall")){
                        if(isVideo){
                            JanusControl.janusControlCreatePeerConnectionFactory(CallActivity.this,rootEglBase);//创建音频，视频
                            JanusControl.sendVideoCallCreateAnswer(CallActivity.this,jsepLocal,isVideo);
                        }else{
                            JanusControl.janusControlCreatePeerConnectionFactory(CallActivity.this);//创建音频
                            JanusControl.sendVideoCallCreateAnswer(CallActivity.this,jsepLocal,isVideo);
                        }
                        //JanusControl.sendPocRoomVideoCallCreateAnswer(CallActivity.this,jsepLocal,isVideo,rootEglBase,CallActivity.this);
                    }else if(msg.getJSONObject("result").getString("event").equals("accepted")){
                        Message message2 = new Message();
                        message2.what = 2;
                        handler.sendMessage(message2);
                    }else if(msg.getJSONObject("result").getString("event").equals("set")){

                    }else if(msg.getJSONObject("result").getString("event").equals("update")){

                    }else if(msg.getJSONObject("result").getString("event").equals("hangup")){
                        if(!isMyHangup && msg.getJSONObject("result").getString("username").equals(name)){
                            Message message3 = new Message();
                            message3.what = 3;
                            Bundle bundle = new Bundle();
                            bundle.putString("name",msg.getJSONObject("result").getString("username"));
                            message3.setData(bundle);//mes利用Bundle传递数据
                            handler.sendMessage(message3);
                        }
                    }
                }else if(msg.getString("videocall").equals("webRtcisok")){//webRtc链接成功

                }
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
                    countDownTimer.cancel();
                    Toast.makeText(CallActivity.this,msg.getData().getString("error"),Toast.LENGTH_SHORT).show();
                    reJoinRoom();
                    break;
                case 1:
                    //calling 播放声音

                    break;
                case 2:
                    //accepted 对方接受 关闭声音，
                    countDownTimer.cancel();
                    connectionIsOk = true;
                    if(mMediaPlayer.isPlaying()){
                        mMediaPlayer.stop();
                    }
                    tvHangup.setText(R.string.audio_call_hangup);
                    tvTime.setVisibility(View.VISIBLE);
                    tvTime.setBase(SystemClock.elapsedRealtime());//计时器清零
                    int hour = (int) ((SystemClock.elapsedRealtime() - tvTime.getBase()) / 1000 / 60);
                    tvTime.setFormat("0"+String.valueOf(hour)+":%s");
                    tvTime.start();
                    break;
                case 3:
                    //hangup 挂断
                    countDownTimer.cancel();
                    if(msg.getData().getString("name").equals(name) && isShowTips){
                        if(!connectionIsOk && mMediaPlayer.isPlaying()){
                            mMediaPlayer.stop();
                        }
                        Toast.makeText(CallActivity.this,R.string.audio_call_hangup_remote_tip,Toast.LENGTH_SHORT).show();
                        reJoinRoom();
                    }
                    break;
                case 4:
                    countDownTimer.cancel();
                    if(mMediaPlayer.isPlaying()){
                        mMediaPlayer.stop();
                    }
                    disConnect();
                    break;
                case 5:
                    countDownTimer.cancel();
                    close();
                    break;
                case 100:
                    countDownTimer.cancel();
                    Toast.makeText(CallActivity.this,msg.getData().getString("error"),Toast.LENGTH_SHORT).show();
                    reJoinRoom();
                    break;
                case 102:
                    countDownTimer.cancel();
                    Toast.makeText(CallActivity.this,R.string.audio_call_hangup_tip,Toast.LENGTH_SHORT).show();
                    if(isCall){
                        reJoinRoom();
                    }else{
                        disConnect();
                    }
                    break;
                case 103:
                    countDownTimer.cancel();
                    Toast.makeText(CallActivity.this,R.string.audio_call_hangup_remote_tip,Toast.LENGTH_SHORT).show();
                    reJoinRoom();
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
        close();
        super.onDestroy();
    }

    private void reJoinRoom(){
        if(isCall){
            if(connectionIsOk){
                JanusControl.janusControlDetach(this,true);
               // JanusControl.janusControlPocRoomDetach(this);
            }else{
                JanusControl.sendAttachPocRoomPlugin(CallActivity.this,true);
            }
        }else{
            if(connectionIsOk){
                JanusControl.janusControlDetach(this,true);
                //JanusControl.janusControlPocRoomDetach(this);
            }
        }
    }

    private void disConnect() {
        Thread myThread=new Thread(){
            @Override
            public void run() {
                try{
                    sleep(2000);
                    Message message5 = new Message();
                    message5.what = 5;
                    handler.sendMessage(message5);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        };
        myThread.start();
    }

    private void close(){
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

        CallActivity.this.finish();
    }

}
