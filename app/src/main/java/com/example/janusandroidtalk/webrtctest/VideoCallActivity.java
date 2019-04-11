package com.example.janusandroidtalk.webrtctest;

import android.app.Activity;
import android.opengl.EGLContext;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import com.example.janusandroidtalk.R;
import com.example.janusandroidtalk.webrtctest.util.SystemUiHider;

import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;


public class VideoCallActivity extends Activity {
    private static final boolean AUTO_HIDE = true;

    private VideoCallTest videoCallTest;

    private EditText editName =null;
    private EditText editCallName =null;

    private Button btnJoin1 = null;
    private Button btnJoin2 = null;
    private Button btnJoin3 = null;

    private String name;
    private String callName;

    private GLSurfaceView vsv;
    private VideoRenderer.Callbacks localRender;
    private VideoRenderer.Callbacks remoteRenders[] = new VideoRenderer.Callbacks[1];

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


    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;

    private class MyInit implements Runnable {

        public void run() {
            init();
        }

        private void init() {
            try {
                EGLContext con = VideoRendererGui.getEGLContext();
                videoCallTest = new VideoCallTest(localRender, remoteRenders);
                videoCallTest.initializeMediaContext(VideoCallActivity.this, true, true, true, con);
                videoCallTest.Start();
            } catch (Exception ex) {
                Log.e("computician.janusclient", ex.getMessage());
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.setProperty("java.net.preferIPv6Addresses", "false");
        System.setProperty("java.net.preferIPv4Stack", "true");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.z_test_activity_video);

        editName = (EditText) findViewById(R.id.vname);
        editCallName = (EditText) findViewById(R.id.vcallname);


        btnJoin1 = (Button) findViewById(R.id.vbutton1);
        btnJoin2 = (Button) findViewById(R.id.vbutton2);
        btnJoin3 = (Button) findViewById(R.id.vbutton3);

        vsv = (GLSurfaceView) findViewById(R.id.glview);
        vsv.setPreserveEGLContextOnPause(true);
        vsv.setKeepScreenOn(true);


        VideoRendererGui.setView(vsv, new MyInit());
        localRender = VideoRendererGui.create(72, 0, 25, 25, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false);
        remoteRenders[0] = VideoRendererGui.create(0, 0, 25, 25, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true);


        btnJoin1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               //注册
                videoCallTest.sendRegister(TextUtils.isEmpty(editName.getText().toString())?"song":editName.getText().toString());

            }
        });
        btnJoin2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //呼叫
                videoCallTest.sendRegister(TextUtils.isEmpty(editName.getText().toString())?"lee":editName.getText().toString());
                //进入calling，对方收到incomingcall，并发送accept，然后收到accepted信令
                //VideoRendererGui.setView(vsv, new MyInit());
            }
        });
        btnJoin3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               //挂断
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
