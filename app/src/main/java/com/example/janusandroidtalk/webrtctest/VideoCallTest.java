package com.example.janusandroidtalk.webrtctest;

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
import computician.janusclientapi.IPluginHandleWebRTCCallbacks;
import computician.janusclientapi.JanusMediaConstraints;
import computician.janusclientapi.JanusPluginHandle;
import computician.janusclientapi.JanusServer;
import computician.janusclientapi.JanusSupportedPluginPackages;
import computician.janusclientapi.PluginHandleSendMessageCallbacks;

/**
 * Created by ben.trent on 7/24/2015.
 */

//TODO create message classes unique to this plugin

public class VideoCallTest {

    //janus 地址，通过ws连接
    private final String JANUS_URI = "ws://113.105.153.240:8188";
    //
    private JanusPluginHandle handle = null;
    private final VideoRenderer.Callbacks localRender, remoteRender;
    private final JanusServer janusServer;

    public class JanusGlobalCallbacks implements IJanusGatewayCallbacks {

        @Override
        public void onSuccess() {
            janusServer.Attach(new JanusPluginCallbacks());
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

    public class JanusPluginCallbacks implements IJanusPluginCallbacks {

        @Override
        public void success(JanusPluginHandle pluginHandle) {
           handle = pluginHandle;

                JSONObject msg = new JSONObject();
                JSONObject obj = new JSONObject();
                try {
                    obj.put("request","register");
                    obj.put("username", "xiaozhu");
                    msg.put("message", obj);
                    handle.sendMessage(new PluginHandleSendMessageCallbacks(msg));
                } catch (Exception ex) {

                }

//            handle.createOffer(new IPluginHandleWebRTCCallbacks() {
//                @Override
//                public JSONObject getJsep() {
//                    return null;
//                }
//
//                @Override
//                public void onCallbackError(String error) {
//
//                }
//
//                @Override
//                public Boolean getTrickle() {
//                    return true;
//                }
//
//                @Override
//                public JanusMediaConstraints getMedia() {
//                    return new JanusMediaConstraints();
//                }
//
//                @Override
//                public void onSuccess(JSONObject obj) {
//                    Log.d("JANUSCLIENT", "OnSuccess for CreateOffer called");
//                    try {
//                        JSONObject body = new JSONObject();
//                        JSONObject msg = new JSONObject();
//                        body.put("request", "call");
//                        body.put("username", "xiaozhuzhu");
//                        msg.put("message", body);
//                        msg.put("jsep", obj);
//                        handle.sendMessage(new PluginHandleSendMessageCallbacks(msg));
//                    } catch (Exception ex) {
//
//                    }
//                }
//            });

        }

        @Override
        public void onMessage(JSONObject msg, final JSONObject jsepLocal) {
            try {
                if(msg.getString("videocall").equals("event")){
                    if(msg.getJSONObject("result").getString("event").equals("incomingcall")){
                        handle.createAnswer(new IPluginHandleWebRTCCallbacks() {
                            @Override
                            public JSONObject getJsep() {
                                return jsepLocal;
                            }

                            @Override
                            public void onCallbackError(String error) {

                            }

                            @Override
                            public Boolean getTrickle() {
                                return true;
                            }

                            @Override
                            public JanusMediaConstraints getMedia() {
                                return new JanusMediaConstraints();
                            }

                            @Override
                            public void onSuccess(JSONObject obj) {
                                try {
                                    JSONObject body = new JSONObject();
                                    JSONObject msg = new JSONObject();
                                    body.put("request", "accept");
                                    msg.put("message", body);
                                    msg.put("jsep", obj);
                                    Log.e("--jsep--",obj.toString());
                                    handle.sendMessage(new PluginHandleSendMessageCallbacks(msg));
                                } catch (Exception ex) {
                                }
                            }
                        });
                    }else if(msg.getJSONObject("result").getString("event").equals("accepted")){
                        Log.e("--accepted--","---------accepted----------");
                    } else if(msg.getJSONObject("result").getString("event").equals("hangup")){
                        Log.e("--accepted--","---------accepted----------");
                        handle.hangUp();
                    }
                }

                if(jsepLocal != null && jsepLocal.getString("type").equals("answer")) {
                    handle.handleRemoteJsep(new IPluginHandleWebRTCCallbacks() {
                        final JSONObject myJsep = jsepLocal;
                        @Override
                        public void onSuccess(JSONObject obj) {
                            Log.d("JANUSCLIENT", "OnSuccess for CreateOffer called");
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
            }catch (Exception e){

            }
        }

        @Override
        public void onLocalStream(MediaStream stream) {
            stream.videoTracks.get(0).addRenderer(new VideoRenderer(localRender));
            VideoRendererGui.update(localRender, 0, 0, 100, 100, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false);
        }

        @Override
        public void onRemoteStream(MediaStream stream) {
            stream.videoTracks.get(0).setEnabled(true);
            if(stream.videoTracks.get(0).enabled())
                Log.d("JANUSCLIENT", "video tracks enabled");
            stream.videoTracks.get(0).addRenderer(new VideoRenderer(remoteRender));
            VideoRendererGui.update(remoteRender, 0, 0, 100, 100, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true);
            VideoRendererGui.update(localRender, 72, 0, 25, 25, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false);
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

    public VideoCallTest(VideoRenderer.Callbacks localRender, VideoRenderer.Callbacks remoteRender) {
        this.localRender = localRender;
        this.remoteRender = remoteRender;
        janusServer = new JanusServer(new JanusGlobalCallbacks());
    }

    public boolean initializeMediaContext(Context context, boolean audio, boolean video, boolean videoHwAcceleration, EGLContext eglContext){
        return janusServer.initializeMediaContext(context, audio, video, videoHwAcceleration, eglContext);
    }

    public void Start() {
        janusServer.Connect();
    }
}
