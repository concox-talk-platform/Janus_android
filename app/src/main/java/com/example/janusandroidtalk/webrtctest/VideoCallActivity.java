package com.example.janusandroidtalk.webrtctest;

import android.app.Activity;
import android.opengl.EGLContext;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.telecom.InCallService;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import com.example.janusandroidtalk.R;
import com.example.janusandroidtalk.webrtctest.util.SystemUiHider;

import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;

public class VideoCallActivity extends Activity {
    private static final boolean AUTO_HIDE = true;

    private GLSurfaceView vsv;
    private VideoRenderer.Callbacks localRender;
    private VideoRenderer.Callbacks remoteRender;
    private VideoCallTest videoCallTest;

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

    private class MyInit implements Runnable {

        public void run() {
            init();
        }

        private void init() {
            try {
                EGLContext con = VideoRendererGui.getEGLContext();
                videoCallTest = new VideoCallTest(localRender, remoteRender);
                videoCallTest.initializeMediaContext(VideoCallActivity.this,true,true,true,con);
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
        setContentView(R.layout.z_test_activity_janus);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        vsv = (GLSurfaceView) findViewById(R.id.glview);
        vsv.setPreserveEGLContextOnPause(true);
        vsv.setKeepScreenOn(true);

        VideoRendererGui.setView(vsv, new MyInit());
        localRender = VideoRendererGui.create(0, 0, 100, 100, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false);
        remoteRender = VideoRendererGui.create(72, 0, 25, 25, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true);

    }
}
