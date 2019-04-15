package com.example.janusandroidtalk.signalingcontrol;

import android.content.Context;
import android.opengl.EGLContext;
import android.util.Log;

import org.json.JSONObject;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;

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
public class JanusControl {
    public static final String REQUEST = "request";//信令，join,start,configure等
    public static final String MESSAGE = "message";//封装 json格式的message,通过ws发送的消息
    public static String JANUS_URI = "ws://113.105.153.240:8188";//websocket server
    public static JanusPluginHandle handle = null;
    public static JanusServer janusServer;
    public static MyControlCallBack controlCallBack;


    private static String userName;
    private static int roomId;
    private static int userId;

    private static  VideoRenderer.Callbacks localRender, remoteRender;

    public JanusControl(MyControlCallBack controlCallBack, String userName, int userId, int roomId) {
        this.controlCallBack = controlCallBack;
        this.userName = userName;
        this.userId = userId;
        this.roomId = roomId;
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
            body.put("muted", true);
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
              if(msg.getString("videocall").equals("event")){// 信令成功
                  if(msg.getJSONObject("result").getString("event").equals("registered")){
                      controlCallBack.showMessage(msg ,jsepLocal);
                  }else if(msg.getJSONObject("result").getString("event").equals("calling")){
                      controlCallBack.showMessage(msg ,jsepLocal);
                  }else if(msg.getJSONObject("result").getString("event").equals("incomingcall")){
                      controlCallBack.showMessage(msg ,jsepLocal);
                  }else if(msg.getJSONObject("result").getString("event").equals("accepted")){
                      controlCallBack.showMessage(msg ,jsepLocal);
                  }else if(msg.getJSONObject("result").getString("event").equals("set")){
                      controlCallBack.showMessage(msg ,jsepLocal);
                  }else if(msg.getJSONObject("result").getString("event").equals("update")){
                      controlCallBack.showMessage(msg ,jsepLocal);
                  }else if(msg.getJSONObject("result").getString("event").equals("hangup")){
                      controlCallBack.showMessage(msg ,jsepLocal);
                  }
              }

              if(jsepLocal != null && jsepLocal.getString("type").equals("answer")) {
                  handle.handleRemoteJsep(new IPluginHandleWebRTCCallbacks() {
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
                          return null;
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
          stream.videoTracks.get(0).addRenderer(new VideoRenderer(localRender));
          VideoRendererGui.update(localRender, 2, 2, 30, 30, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false);
      }

      @Override
      public void onRemoteStream(MediaStream stream) {
          stream.videoTracks.get(0).setEnabled(true);
          if(stream.videoTracks.get(0).enabled())
              Log.d("JANUSCLIENT", "video tracks enabled");
          stream.videoTracks.get(0).addRenderer(new VideoRenderer(remoteRender));
          VideoRendererGui.update(remoteRender, 68, 2, 30, 30, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true);
          VideoRendererGui.update(localRender, 2, 2, 30, 30, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false);
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
    public static void sendVideoCallCreateOffer(final MyControlCallBack myControlCallBack,final String remoteName , VideoRenderer.Callbacks mlocalRender, VideoRenderer.Callbacks mremoteRender){
        localRender = mlocalRender;
        remoteRender = mremoteRender;
        if(handle != null) {
            handle.createOffer(new IPluginHandleWebRTCCallbacks() {
                @Override
                public void onSuccess(JSONObject obj) {
                    try {
                        controlCallBack = myControlCallBack;
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
                    cons.setRecvAudio(true);
                    cons.setRecvVideo(true);
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

    public static void sendVideoCallCreateAnswer(final MyControlCallBack myControlCallBack , final JSONObject jsepLocal , VideoRenderer.Callbacks mlocalRender, VideoRenderer.Callbacks mremoteRender){
        if(handle != null) {
            localRender = mlocalRender;
            remoteRender = mremoteRender;
            handle.createAnswer(new IPluginHandleWebRTCCallbacks() {
                @Override
                public void onSuccess(JSONObject obj) {
                    try {
                        controlCallBack = myControlCallBack;
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

}

