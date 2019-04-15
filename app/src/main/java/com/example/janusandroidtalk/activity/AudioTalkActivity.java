package com.example.janusandroidtalk.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.janusandroidtalk.R;
import com.example.janusandroidtalk.signalingcontrol.AudioBridgeControl;
import com.example.janusandroidtalk.signalingcontrol.MyControlCallBack;
import com.example.janusandroidtalk.webrtctest.AppRTCAudioManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;


public class AudioTalkActivity extends Activity implements MyControlCallBack {

    private AppRTCAudioManager audioManager = null;
    private Button btnSpeak = null;
    private String myid ="";

    private TextView title = null;
    private ImageView back = null;
    private String name="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.setProperty("java.net.preferIPv6Addresses", "false");
        System.setProperty("java.net.preferIPv4Stack", "true");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_layout_group_talk);

        name = getIntent().getStringExtra("name");

        title = (TextView) findViewById(R.id.group_talk_title);
        back = (ImageView) findViewById(R.id.group_talk_back);

        title.setText(name);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AudioTalkActivity.this.finish();
            }
        });


        Random random = new Random();
        for (int i=0;i<6;i++)
        {
            myid+=random.nextInt(10);
        }
        btnSpeak = (Button) findViewById(R.id.layout_group_talk_speak);

        btnSpeak.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN: {

                            AudioBridgeControl.sendTalk(AudioTalkActivity.this);
                            break;
                        }
                        case MotionEvent.ACTION_MOVE: {

                            break;
                        }
                        case MotionEvent.ACTION_UP: {
                            btnSpeak.setText("按住说话");

                            //AudioBridgeControl.stopSpeak();
                            AudioBridgeControl.sendUnTalk(AudioTalkActivity.this);

                            break;
                        }
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
                        if(msg.getString("audiobridge").equals("talked")){
                            if(msg.getInt("talk") == 0 || (msg.getInt("talk") == Integer.parseInt("1"+myid)) ){
                                btnSpeak.setText("正在说话...");
                                //AudioBridgeControl.startSpeak();
                            }else{
                                Toast.makeText(AudioTalkActivity.this,msg.getInt("talk")+"正在说话，请稍后",Toast.LENGTH_SHORT).show();
                            }
                        }else if(msg.getString("audiobridge").equals("untalked")){
                            Toast.makeText(AudioTalkActivity.this,"讲话结束",Toast.LENGTH_SHORT).show();
                        }else{

                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });

    }
}
