package com.example.janusandroidtalk.signalingcontrol;

import org.json.JSONObject;

public interface MyControlCallBack {
    void janusServer(Boolean isOk);
    void showMessage(JSONObject msg,JSONObject jsepLocal);
}
