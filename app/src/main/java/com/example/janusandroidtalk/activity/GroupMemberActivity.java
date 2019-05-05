package com.example.janusandroidtalk.activity;

import android.support.v7.app.AppCompatActivity;

import com.example.janusandroidtalk.signalingcontrol.MyControlCallBack;

import org.json.JSONObject;
import org.webrtc.MediaStream;

public class GroupMemberActivity extends AppCompatActivity implements MyControlCallBack {
    @Override
    public void janusServer(int code, String msg) {

    }

    @Override
    public void showMessage(JSONObject msg, JSONObject jsepLocal) {

    }

    @Override
    public void onSetLocalStream(MediaStream stream) {

    }

    @Override
    public void onAddRemoteStream(MediaStream stream) {

    }
}
