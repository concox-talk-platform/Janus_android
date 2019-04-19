package com.example.janusandroidtalk.signalingcontrol;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.opengl.EGLContext;
import android.os.Bundle;
import android.util.Log;

import com.example.janusandroidtalk.MainActivity;
import com.example.janusandroidtalk.MyApplication;
import com.example.janusandroidtalk.activity.CallActivity;

import org.json.JSONObject;
import org.webrtc.EglBase;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
public class JanusControl {
    public static final String REQUEST = "request";//信令，join,start,configure等
    public static final String MESSAGE = "message";//封装 json格式的message,通过ws发送的消息
    public static String JANUS_URI = "ws://113.105.153.240:9188";//websocket server
    public static JanusPluginHandle handle = null;
    public static JanusServer janusServer;
    public static MyControlCallBack controlCallBack;


    private static String userName;
    private static int roomId;
    private static int userId;

    public JanusControl(MyControlCallBack controlCallBack, String userName, int userId, int roomId) {
        this.controlCallBack = controlCallBack;
        this.userName = userName;
        this.userId = userId;
        this.roomId = roomId;
        janusServer = new JanusServer(new JanusGlobalCallbacks());
    }

    public void Start() {
        janusServer.Connect();
    }

    //全局的janus回调函数类
    public class JanusGlobalCallbacks implements IJanusGatewayCallbacks {
        public void onSuccess() {
            controlCallBack.janusServer(true);
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
            controlCallBack.janusServer(false);
        }
    }

    //使用pocRoom插件
    public static void sendAttachPocRoomPlugin(final MyControlCallBack myControlCallBack) {
        controlCallBack = myControlCallBack;
        janusServer.Attach(new JanusPocRoomPluginCallbacks());
    }

    //使用VideoCall插件
    public static void sendAttachVideoCalllugin(final MyControlCallBack myControlCallBack) {
        controlCallBack = myControlCallBack;
        janusServer.Attach(new JanusVideoCallPluginCallbacks());
    }

    //关闭webSocket连接
    public static void closeJanusServer(){
        janusServer.Destroy();
    }
    //关闭webrtc连接
    public static void closeWebRtc(){
        handle.hangUp();
    }
    /**
     * poc Room插件---------------
     * */
    public static class JanusPocRoomPluginCallbacks implements IJanusPluginCallbacks {

        @Override
        public void success(JanusPluginHandle pluginHandle) {
            handle = pluginHandle;
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("pocroom", "audiobridgeisok");
                controlCallBack.showMessage(jsonObject,null);
            }catch (Exception e){

            }
        }

        @Override
        public void onMessage(JSONObject msg, JSONObject jsepLocal) {
            try {
                controlCallBack.showMessage(msg,null);
                if(jsepLocal != null && jsepLocal.getString("type").equals("answer")) {
                    handle.JanusSetRemoteDescription(new IPluginHandleWebRTCCallbacks() {
                        final JSONObject myJsep = jsepLocal;
                        @Override
                        public void onSuccess(JSONObject obj) {

                        }

                        @Override
                        public JSONObject getJsep() {
                            return myJsep;
                        }

                        @Override
                        public JanusMediaConstraints getMedia() {
                            JanusMediaConstraints cons = new JanusMediaConstraints();
                            cons.setRecvAudio(false);
                            cons.setRecvVideo(false);
                            cons.setSendAudio(true);
                            cons.setSendVideo(false);
                            return cons;
                        }

                        @Override
                        public Boolean getTrickle() {
                            return Boolean.FALSE;
                        }

                        @Override
                        public void onCallbackError(String error) {

                        }
                    });
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
                controlCallBack.showMessage(jsonObject,null);
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

    //发送pocRoom Offer信令
    public static void sendPocRoomCreateOffer(final MyControlCallBack myControlCallBack){
        if(handle != null) {
            handle.createPeerConnection(new IPluginHandleWebRTCCallbacks() {
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
                    cons.setSendVideo(false);
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

    //发送加入群组信令
    public static void sendPocRoomJoinRoom(final MyControlCallBack myControlCallBack,int myroomid){
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
                    myControlCallBack.showMessage(obj,null);
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
                    myControlCallBack.showMessage(obj,null);
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
                    myControlCallBack.showMessage(obj,null);
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
            body.put("muted", false);
            msg.put(MESSAGE, body);
            handle.sendMessage(new IPluginHandleSendMessageCallbacks() {
                @Override
                public void onSuccessSynchronous(JSONObject obj) {
                    myControlCallBack.showMessage(obj,null);
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
                    myControlCallBack.showMessage(obj,null);
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
    /**
     * poc Room插件---------------
     * */

    /**
    * video Call插件----
    * */
    public static class JanusVideoCallPluginCallbacks implements IJanusPluginCallbacks {

      @Override
      public void success(JanusPluginHandle pluginHandle) {
          handle = pluginHandle;
          try {
              JSONObject jsonObject = new JSONObject();
              jsonObject.put("videocall", "videoCallIsOk");
              controlCallBack.showMessage(jsonObject,null);
          }catch (Exception e){

          }
      }

      @Override
      public void onMessage(JSONObject msg,final JSONObject jsepLocal) {
          try {
              if(msg.has("result") && jsepLocal != null){
                  if(msg.getJSONObject("result").getString("event").equals("incomingcall")){
                      Intent intent = new Intent(getCurrentActivity(), CallActivity.class);
                      Bundle bundle = new Bundle();
                      bundle.putString("name", msg.getJSONObject("result").getString("username"));
                      bundle.putString("jsep",jsepLocal.toString());
                      bundle.putBoolean("isCall",false);
                      intent.putExtras(bundle);
                      getCurrentActivity().startActivity(intent);
                  }else{
                      controlCallBack.showMessage(msg ,jsepLocal);
                  }
              }else{
                  controlCallBack.showMessage(msg ,jsepLocal);
              }
              if(jsepLocal != null && jsepLocal.getString("type").equals("answer")) {
                  handle.JanusSetRemoteDescription(new IPluginHandleWebRTCCallbacks() {
                      final JSONObject myJsep = jsepLocal;
                      @Override
                      public void onSuccess(JSONObject obj) {

                      }

                      @Override
                      public JSONObject getJsep() {
                          return myJsep;
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
                          return Boolean.FALSE;
                      }

                      @Override
                      public void onCallbackError(String error) {

                      }
                  });
              }

          } catch (Exception ex) {

          }
      }

        @Override
        public void onLocalStream(MediaStream stream) {
            controlCallBack.onSetLocalStream(stream);
        }

        @Override
        public void onRemoteStream(MediaStream stream) {
            controlCallBack.onAddRemoteStream(stream);
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
          return JanusSupportedPluginPackages.JANUS_VIDEO_CALL;
      }

      @Override
      public void onCallbackError(String error) {

      }

      @Override
      public void onDetached() {

      }
  }
    public static void janusControlCreatePeerConnectionFactory(Context context,EglBase rootEglBase){
        handle.createPeerConnectionFactory(context,rootEglBase);
    }

    public static void janusControlCreatePeerConnectionFactory(Context context){
        handle.createPeerConnectionFactory(context);
    }

    //register信令
    public static void sendRegister(){
        try {
            JSONObject msg = new JSONObject();
            JSONObject body = new JSONObject();
            body.put(REQUEST, "register");
            body.put("username", userName);
            msg.put(MESSAGE, body);
            handle.sendMessage(new PluginHandleSendMessageCallbacks(msg));
        }catch (Exception ex) {

        }
    }

    //call信令
    public static void sendCall(MyControlCallBack myControlCallBack,String remoteName){
        try {
            controlCallBack = myControlCallBack;
            JSONObject msg = new JSONObject();
            JSONObject body = new JSONObject();
            body.put(REQUEST, "call");
            body.put("username", remoteName);
            msg.put(MESSAGE, body);
            handle.sendMessage(new PluginHandleSendMessageCallbacks(msg));
        }catch (Exception ex) {

        }
    }

    //accept信令
    public static void sendAccept(JSONObject obj){
        try {
            JSONObject msg = new JSONObject();
            JSONObject body = new JSONObject();
            body.put(REQUEST, "set");
            body.put("video", false);
            body.put("audio", true);
            msg.put(MESSAGE, body);
            msg.put("jsep", obj);
            handle.sendMessage(new PluginHandleSendMessageCallbacks(msg));
        }catch (Exception ex) {

        }
    }

    //offer信令
    public static void sendVideoCallCreateOffer(final MyControlCallBack myControlCallBack,final String remoteName){
        if(handle != null) {
            controlCallBack = myControlCallBack;
            handle.createPeerConnection(new IPluginHandleWebRTCCallbacks() {
                @Override
                public void onSuccess(JSONObject obj) {
                    try {
                        JSONObject msg = new JSONObject();
                        JSONObject body = new JSONObject();
                        body.put(REQUEST, "call");
                        body.put("username", remoteName);
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

    public static void sendVideoCallCreateAnswer(final MyControlCallBack myControlCallBack , final JSONObject jsepLocal ){
        controlCallBack = myControlCallBack;
        if(handle != null) {
            handle.createPeerConnection(new IPluginHandleWebRTCCallbacks() {
                @Override
                public void onSuccess(JSONObject obj) {
                    try {
                        JSONObject msg = new JSONObject();
                        JSONObject body = new JSONObject();
                        body.put(REQUEST, "accept");
                        msg.put(MESSAGE, body);
                        msg.put("jsep", obj);
                        handle.sendMessage(new PluginHandleSendMessageCallbacks(msg));
                    }catch (Exception ex) {

                    }
                }

                @Override
                public JSONObject getJsep() {
                    return jsepLocal;
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

    //hangup信令
    public static void sendHangup(final MyControlCallBack myControlCallBack){
        try {
            controlCallBack = myControlCallBack;
            JSONObject msg = new JSONObject();
            JSONObject body = new JSONObject();
            body.put(REQUEST, "hangup");
            msg.put(MESSAGE, body);
            handle.sendMessage(new PluginHandleSendMessageCallbacks(msg));
        }catch (Exception ex) {

        }
    }
    /**
     * video Call插件----
     * */

    public static Activity getCurrentActivity () {
        try {
            Class activityThreadClass = Class.forName("android.app.ActivityThread");
            Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(
                    null);
            Field activitiesField = activityThreadClass.getDeclaredField("mActivities");
            activitiesField.setAccessible(true);
            Map activities = (Map) activitiesField.get(activityThread);
            for (Object activityRecord : activities.values()) {
                Class activityRecordClass = activityRecord.getClass();
                Field pausedField = activityRecordClass.getDeclaredField("paused");
                pausedField.setAccessible(true);
                if (!pausedField.getBoolean(activityRecord)) {
                    Field activityField = activityRecordClass.getDeclaredField("activity");
                    activityField.setAccessible(true);
                    Activity activity = (Activity) activityField.get(activityRecord);
                    return activity;
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

}

