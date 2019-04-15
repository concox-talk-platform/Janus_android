package com.example.janusandroidtalk.webrtctest;

import android.app.Activity;
import android.opengl.EGLContext;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.janusandroidtalk.R;
import com.example.janusandroidtalk.signalingcontrol.MyControlCallBack;
import com.example.janusandroidtalk.webrtctest.util.SystemUiHider;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.VideoRendererGui;

import java.util.Random;



public class AudioRoomActivity extends Activity implements MyControlCallBack {
    private static final boolean AUTO_HIDE = true;

    private AudioRoomTest audioRoomTest;

    private EditText editRoom =null;
    private EditText editName =null;
    private EditText myid =null;

    private TextView textGroup =null;
    private TextView textSpeak =null;


    private Button btnJoin = null;
    private Button btnSpeak = null;

    private Button btnJoin1 = null;
    private Button btnJoin2 = null;
    private Button btnJoin3 = null;

    private int roomId;
    private String name;

    private boolean isTalk = false;

    private AppRTCAudioManager audioManager = null;
    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;


    private Button button = null;


    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;

//    private class MyInit implements Runnable {
//
//        public void run() {
//            init();
//        }
//
//        private void init() {
//            try {
//                EGLContext con = VideoRendererGui.getEGLContext();
//                audioRoomTest = new AudioRoomTest(name,roomId);
//                audioRoomTest.initializeMediaContext(AudioRoomActivity.this, true, false, false, con);
//                audioRoomTest.Start();
//            } catch (Exception ex) {
//                Log.e("computician.janusclient", ex.getMessage());
//            }
//        }
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.setProperty("java.net.preferIPv6Addresses", "false");
        System.setProperty("java.net.preferIPv4Stack", "true");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.z_test_activity_audio);

        editRoom = (EditText) findViewById(R.id.room);
        editName = (EditText) findViewById(R.id.name);
        myid = (EditText) findViewById(R.id.myid);

        textGroup = (TextView) findViewById(R.id.group);
        textSpeak = (TextView) findViewById(R.id.speak);

        btnJoin = (Button) findViewById(R.id.join);

        btnJoin1 = (Button) findViewById(R.id.button1);
        btnJoin2 = (Button) findViewById(R.id.button2);
        btnJoin3 = (Button) findViewById(R.id.button3);

        btnSpeak = (Button) findViewById(R.id.button);

        btnJoin1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //audioRoomTest.sendPublisher();
                if(!TextUtils.isEmpty(myid.getText().toString()) && audioManager != null) {
                    audioRoomTest.sendTalk(AudioRoomActivity.this, Integer.parseInt(myid.getText().toString()));
                }
            }
        });
        btnJoin2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!TextUtils.isEmpty(myid.getText().toString()) && audioManager != null){
                    audioRoomTest.sendUnTalk(AudioRoomActivity.this,Integer.parseInt(myid.getText().toString()));
                }

            }
        });
        btnJoin3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        Random random = new Random();
        String result="";
        for (int i=0;i<6;i++)
        {
            result+=random.nextInt(10);
        }

        myid.setText(result);

        btnJoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                name = TextUtils.isEmpty(editName.getText().toString())?"android":editName.getText().toString();
                roomId = Integer.parseInt(TextUtils.isEmpty(editRoom.getText().toString())?"1234":editRoom.getText().toString());

                // Create and audio manager that will take care of audio routing,
                // audio modes, audio device enumeration etc.
                audioManager = AppRTCAudioManager.create(AudioRoomActivity.this, new Runnable() {
                            // This method will be called each time the audio state (number and
                            // type of devices) has been changed.
                            @Override
                            public void run() {
                                onAudioManagerChangedState();
                            }
                        }
                );
                // Store existing audio settings and change audio mode to
                // MODE_IN_COMMUNICATION for best possible VoIP performance.
                audioManager.init();

                EGLContext con = VideoRendererGui.getEGLContext();
                audioRoomTest = new AudioRoomTest();
                audioRoomTest.initializeMediaContext(AudioRoomActivity.this, true, false, false, con);
                audioRoomTest.Start();
                btnJoin.setText("已经加入房间");
                btnJoin.setEnabled(false);
            }
        });

        btnSpeak.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(!TextUtils.isEmpty(myid.getText().toString())){
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN: {
                            if(audioRoomTest != null){
                                audioRoomTest.sendTalk(AudioRoomActivity.this,Integer.parseInt("1"+myid.getText().toString()));
                            }

                            break;
                        }
                        case MotionEvent.ACTION_MOVE: {

                            break;
                        }
                        case MotionEvent.ACTION_UP: {
                            btnSpeak.setText("按住说话");
                            if(audioRoomTest != null){
                                //audioRoomTest.stopSpeak();
                                audioRoomTest.sendUnTalk(AudioRoomActivity.this,Integer.parseInt("1"+myid.getText().toString()));
                            }
                            break;
                        }
                    }
                }else{
                    Toast.makeText(AudioRoomActivity.this,"请输入您的ID",Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        });

    }

    private void onAudioManagerChangedState() {
        // TODO(henrika): disable video if AppRTCAudioManager.AudioDevice.EARPIECE
        // is active.
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (audioManager != null) {
            audioManager.close();
            audioManager = null;
        }
    }

    @Override
    public void janusServer(Boolean isOk) {

    }

    //回调信息处理，
    @Override
    public void showMessage(final JSONObject msg,JSONObject jsepLocal) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try{
                        if(msg.getString("pocroom").equals("talked")){
                            if(msg.getInt("talk") == 0 || (msg.getInt("talk") == Integer.parseInt("1"+myid.getText().toString())) ){
                                isTalk = true;
                                btnSpeak.setText("正在说话...");
                                if(audioRoomTest != null){
                                    //audioRoomTest.startSpeak();
                                }
                            }else{
                                Toast.makeText(AudioRoomActivity.this,msg.getInt("talk")+"正在说话，请稍后",Toast.LENGTH_SHORT).show();
                            }
                        }else if(msg.getString("pocroom").equals("untalked")){
                            Toast.makeText(AudioRoomActivity.this,"讲话结束",Toast.LENGTH_SHORT).show();
                        }else{
                            isTalk = false;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });

    }
}
