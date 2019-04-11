package com.example.janusandroidtalk.signalingcontrol;

import android.content.Context;
import android.opengl.EGLContext;

import org.json.JSONArray;
import org.json.JSONObject;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import computician.janusclientapi.IJanusGatewayCallbacks;
import computician.janusclientapi.IJanusPluginCallbacks;
import computician.janusclientapi.IPluginHandleSendMessageCallbacks;
import computician.janusclientapi.IPluginHandleWebRTCCallbacks;
import computician.janusclientapi.JanusMediaConstraints;
import computician.janusclientapi.JanusPluginHandle;
import computician.janusclientapi.JanusServer;
import computician.janusclientapi.JanusSupportedPluginPackages;
import computician.janusclientapi.PluginHandleSendMessageCallbacks;
import computician.janusclientapi.PluginHandleWebRTCCallbacks;

//TODO create message classes unique to this plugin

/**
 * Created by ben.trent on 7/24/2015.
 */
public class AudioBridgeControl {
    public static final String REQUEST = "request";//信令，join,start,configure等
    public static final String MESSAGE = "message";//封装 json格式的message,通过ws发送的消息
    public static String JANUS_URI = "ws://113.105.153.240:9188";//websocket server
    public static JanusPluginHandle handle = null;
    public static JanusServer janusServer;
    public static MyControlCallBack controlCallBack;

    //通过构造函数获取
    private static String userName;
    private static int roomId;
    private static int userId;

    public AudioBridgeControl(String userName,int userId ,int roomId,MyControlCallBack controlCallBack) {
        this.userName = userName;
        this.userId = userId;
        this.roomId = roomId;
        this.controlCallBack = controlCallBack;
    }

    public boolean initializeMediaContext(Context context, boolean audio, boolean video, boolean videoHwAcceleration, EGLContext eglContext){
        janusServer = new JanusServer(new JanusGlobalCallbacks());
        return janusServer.initializeMediaContext(context, audio, video, videoHwAcceleration, eglContext);
    }

    public void Start() {
        janusServer.Connect();
    }

    //全局的janus回调函数类
    public class JanusGlobalCallbacks implements IJanusGatewayCallbacks {
        public void onSuccess() {
            janusServer.Attach(new JanusPublisherPluginCallbacks());
        }

        @Override
        public void onDestroy() {
        }

        @Override
        public String getServerUri() {
            return JANUS_URI;
        }

        @Override
        public List<PeerConnection.IceServer> getIceServers() {
            return new ArrayList<PeerConnection.IceServer>();
        }

        @Override
        public Boolean getIpv6Support() {
            return Boolean.FALSE;
        }

        @Override
        public Integer getMaxPollEvents() {
            return 0;
        }

        @Override
        public void onCallbackError(String error) {

        }
    }

    public class JanusPublisherPluginCallbacks implements IJanusPluginCallbacks {

        @Override
        public void success(JanusPluginHandle pluginHandle) {
            handle = pluginHandle;
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("pocroom", "audiobridgeisok");
                controlCallBack.showMessage(jsonObject);
            }catch (Exception e){

            }
        }

        @Override
        public void onMessage(JSONObject msg, JSONObject jsepLocal) {
            try {
                controlCallBack.showMessage(msg);
                if(jsepLocal != null) {
                    handle.handleRemoteJsep(new PluginHandleWebRTCCallbacks(null, jsepLocal, false));
                }
            } catch (Exception ex) {

            }
        }

        @Override
        public void onLocalStream(MediaStream stream) {

        }

        @Override
        public void onRemoteStream(MediaStream stream) {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("pocroom", "webRtcisok");
                controlCallBack.showMessage(jsonObject);
            }catch (Exception e){

            }
        }

        @Override
        public void onDataOpen(Object data) {

        }

        @Override
        public void onData(Object data) {

        }

        @Override
        public void onCleanup() {

        }

        @Override
        public JanusSupportedPluginPackages getPlugin() {
            return JanusSupportedPluginPackages.JANUS_POC_ROOM;
        }

        @Override
        public void onCallbackError(String error) {

        }

        @Override
        public void onDetached() {

        }
    }

    public static void sendCreateOffer(final MyControlCallBack myControlCallBack){
        if(handle != null) {
            handle.createOffer(new IPluginHandleWebRTCCallbacks() {
                @Override
                public void onSuccess(JSONObject obj) {
                    try
                    {
                        JSONObject msg = new JSONObject();
                        JSONObject body = new JSONObject();
                        body.put(REQUEST, "configure");
                        body.put("muted", true);
                        body.put("display", userName);
                        msg.put(MESSAGE, body);
                        msg.put("jsep", obj);
                        handle.sendMessage(new PluginHandleSendMessageCallbacks(msg));
                    }catch (Exception ex) {

                    }
                }

                @Override
                public JSONObject getJsep() {
                    return null;
                }

                @Override
                public JanusMediaConstraints getMedia() {
                    JanusMediaConstraints cons = new JanusMediaConstraints();
                    cons.setRecvAudio(false);
                    cons.setRecvVideo(false);
                    cons.setSendAudio(true);
                    return cons;
                }

                @Override
                public Boolean getTrickle() {
                    return true;
                }

                @Override
                public void onCallbackError(String error) {

                }
            });
        }
    }


    public static void sendJoinRoom(final MyControlCallBack myControlCallBack,int myroomid){
        if(handle != null) {
            controlCallBack = myControlCallBack;
            roomId = myroomid;
            JSONObject obj = new JSONObject();
            JSONObject msg = new JSONObject();
            try {
                obj.put(REQUEST, "join");
                obj.put("room", roomId);
                obj.put("id", userId);
                obj.put("muted", true);
                obj.put("display",userName);
                msg.put(MESSAGE, obj);
            } catch(Exception ex) {

            }
            handle.sendMessage(new PluginHandleSendMessageCallbacks(msg));
        }
    }


    //发送获取talk权限信令
    public static void sendTalk(final MyControlCallBack myControlCallBack){
        try {
            final JSONObject msg = new JSONObject();
            JSONObject body = new JSONObject();
            body.put(REQUEST, "talk");
            body.put("room", roomId);
            body.put("id", userId);
            msg.put(MESSAGE, body);
            handle.sendMessage(new IPluginHandleSendMessageCallbacks() {
                @Override
                public void onSuccessSynchronous(JSONObject obj) {
                    myControlCallBack.showMessage(obj);
                }

                @Override
                public void onSuccesAsynchronous() {

                }

                @Override
                public JSONObject getMessage() {
                    return msg;
                }

                @Override
                public void onCallbackError(String error) {

                }
            });
        }catch (Exception ex) {

        }
    }

    //发送unTalk放麦信令
    public static void sendUnTalk(final MyControlCallBack myControlCallBack){
        try {
            final JSONObject msg = new JSONObject();
            JSONObject body = new JSONObject();
            body.put(REQUEST, "untalk");
            body.put("room", roomId);
            body.put("id",userId);
            //body.put("id", new BigInteger("4188436743763777"));
            msg.put(MESSAGE, body);
            handle.sendMessage(new IPluginHandleSendMessageCallbacks() {
                @Override
                public void onSuccessSynchronous(JSONObject obj) {
                    myControlCallBack.showMessage(obj);
                }

                @Override
                public void onSuccesAsynchronous() {

                }

                @Override
                public JSONObject getMessage() {
                    return msg;
                }

                @Override
                public void onCallbackError(String error) {

                }
            });
        }catch (Exception ex) {

        }
    }

    //发送创建群组信令
    public static void sendCreateGroup(final MyControlCallBack myControlCallBack,int createRoomId){
        try {
            final JSONObject msg = new JSONObject();
            JSONObject body = new JSONObject();
            body.put(REQUEST, "create");
            body.put("room", createRoomId);
            body.put("permanent", true);
            body.put("is_private", false);
            msg.put(MESSAGE, body);
            handle.sendMessage(new IPluginHandleSendMessageCallbacks() {
                @Override
                public void onSuccessSynchronous(JSONObject obj) {
                    myControlCallBack.showMessage(obj);
                }

                @Override
                public void onSuccesAsynchronous() {

                }

                @Override
                public JSONObject getMessage() {
                    return msg;
                }

                @Override
                public void onCallbackError(String error) {

                }
            });
        }catch (Exception ex) {

        }
    }

    //切换房间信令
    public static void sendChangeGroup(final MyControlCallBack myControlCallBack,int changeRoomId){
        try {
            controlCallBack = myControlCallBack;
            final JSONObject msg = new JSONObject();
            JSONObject body = new JSONObject();
            body.put(REQUEST, "changeroom");
            body.put("room", changeRoomId);
            body.put("id", userId);
            body.put("display", userName);
            body.put("muted", true);
            msg.put(MESSAGE, body);
            handle.sendMessage(new IPluginHandleSendMessageCallbacks() {
                @Override
                public void onSuccessSynchronous(JSONObject obj) {
                    myControlCallBack.showMessage(obj);
                }

                @Override
                public void onSuccesAsynchronous() {

                }

                @Override
                public JSONObject getMessage() {
                    return msg;
                }

                @Override
                public void onCallbackError(String error) {

                }
            });
        }catch (Exception ex) {

        }
    }

    //发送配置信令，muted，切换静音
    public static void sendConfigure(final MyControlCallBack myControlCallBack,boolean muted){
        try {
            controlCallBack = myControlCallBack;
            final JSONObject msg = new JSONObject();
            JSONObject body = new JSONObject();
            body.put(REQUEST, "configure");
            body.put("muted", muted);
            body.put("display", userName);
            msg.put(MESSAGE, body);
            handle.sendMessage(new IPluginHandleSendMessageCallbacks() {
                @Override
                public void onSuccessSynchronous(JSONObject obj) {
                    myControlCallBack.showMessage(obj);
                }

                @Override
                public void onSuccesAsynchronous() {

                }

                @Override
                public JSONObject getMessage() {
                    return msg;
                }

                @Override
                public void onCallbackError(String error) {

                }
            });
        }catch (Exception ex) {

        }
    }
}

