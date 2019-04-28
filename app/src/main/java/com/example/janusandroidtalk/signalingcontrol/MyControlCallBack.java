package com.example.janusandroidtalk.signalingcontrol;

import org.json.JSONObject;
import org.webrtc.MediaStream;

public interface MyControlCallBack {
    void janusServer(int code,String msg);
    void showMessage(JSONObject msg,JSONObject jsepLocal);
    void onSetLocalStream(MediaStream stream);
    void onAddRemoteStream(MediaStream stream);
}
