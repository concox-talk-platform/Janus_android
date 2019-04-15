package com.example.janusandroidtalk.webrtctest;

import android.content.Context;
import android.opengl.EGLContext;

import com.example.janusandroidtalk.signalingcontrol.MyControlCallBack;

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
public class AudioRoomTest {
    public static final String REQUEST = "request";//信令，join,start,configure等
    public static final String MESSAGE = "message";//封装 json格式的message,通过ws发送的消息
    public static final String PUBLISHERS = "publishers";//类似于发送方，
    private String JANUS_URI = "ws://113.105.153.240:8188";//websocket server
    //private final String JANUS_URI = "ws://192.168.59.128:8188";//websocket server
    private JanusPluginHandle handle = null;//
    //private VideoRenderer.Callbacks localRender;
   // private Deque<VideoRenderer.Callbacks> availableRemoteRenderers = new ArrayDeque<>();
   // private HashMap<BigInteger, VideoRenderer.Callbacks> remoteRenderers = new HashMap<>();
    private JanusServer janusServer;
    private BigInteger myid;
    private String user_name = "android";
    private int roomid = 1234;

    private static AudioRoomTest audioRoomTest = null;

    public static AudioRoomTest setAudioRoomTest(AudioRoomTest audioRoomTest){
        audioRoomTest = audioRoomTest;
        return audioRoomTest;
    }
    public static AudioRoomTest getAudioRoomTest() {
        return audioRoomTest;
    }
    public static void clearAudioRoomTest(){
        audioRoomTest = null;
    }

    public AudioRoomTest() {
        //this.localRender = localRender;
//        for(int i = 0; i < remoteRenders.length; i++)
//        {
//            this.availableRemoteRenderers.push(remoteRenders[i]);
//        }
        //this.availableRemoteRenderers.push(remoteRenders);\

    }

    class ListenerAttachCallbacks implements IJanusPluginCallbacks{
        //final private VideoRenderer.Callbacks renderer;
        final private BigInteger feedid;
        private JanusPluginHandle listener_handle = null;

        public ListenerAttachCallbacks(BigInteger id){
            //this.renderer = renderer;
            this.feedid = id;
        }

        public void success(JanusPluginHandle handle) {
            listener_handle = handle;
            try
            {
                JSONObject body = new JSONObject();
                JSONObject msg = new JSONObject();
                body.put(REQUEST, "join");
                body.put("room", roomid);
                body.put("ptype", "listener");
                body.put("feed", feedid);
                msg.put(MESSAGE, body);
                handle.sendMessage(new PluginHandleSendMessageCallbacks(msg));
            }
            catch(Exception ex)
            {

            }
        }

        @Override
        public void onMessage(JSONObject msg, JSONObject jsep) {

            try {
                //String event = msg.getString("videoroom");//-----------change--------------
                String event = msg.getString("audiobridge");
                if (event.equals("attached") && jsep != null) {
                    final JSONObject remoteJsep = jsep;
                    listener_handle.createAnswer(new IPluginHandleWebRTCCallbacks() {
                        @Override
                        public void onSuccess(JSONObject obj) {
                            try {
                                JSONObject mymsg = new JSONObject();
                                JSONObject body = new JSONObject();
                                body.put(REQUEST, "start");
                                body.put("room", roomid);
                                mymsg.put(MESSAGE, body);
                                mymsg.put("jsep", obj);
                                listener_handle.sendMessage(new PluginHandleSendMessageCallbacks(mymsg));
                            } catch (Exception ex) {

                            }
                        }

                        @Override
                        public JSONObject getJsep() {
                            return remoteJsep;
                        }

                        @Override
                        public JanusMediaConstraints getMedia() {
                            JanusMediaConstraints cons = new JanusMediaConstraints();
                            cons.setVideo(null);
                            cons.setRecvAudio(true);
                            //cons.setRecvVideo(true);//--------------change-----
                            cons.setRecvVideo(false);
                            cons.setSendAudio(false);
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
            catch(Exception ex)
            {

            }
        }

        @Override
        public void onLocalStream(MediaStream stream) {

        }

        @Override
        public void onRemoteStream(MediaStream stream) {
//            stream.videoTracks.get(0).addRenderer(new VideoRenderer(renderer));//---------change----
//            audioRoomTest = new AudioRoomTest();
//            AudioRoomTest.setAudioRoomTest(audioRoomTest);
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
        public void onDetached() {

        }

        @Override
        public JanusSupportedPluginPackages getPlugin() {
            //return JanusSupportedPluginPackages.JANUS_VIDEO_ROOM;//-------------change------
            return JanusSupportedPluginPackages.JANUS_AUDIO_BRIDGE;
        }

        @Override
        public void onCallbackError(String error) {

        }
    }

    public class JanusPublisherPluginCallbacks implements IJanusPluginCallbacks {

        private void publishOwnFeed() {
            if(handle != null) {
                handle.createOffer(new IPluginHandleWebRTCCallbacks() {
                    @Override
                    public void onSuccess(JSONObject obj) {
                        try
                        {
                            JSONObject msg = new JSONObject();
                            JSONObject body = new JSONObject();
                            body.put(REQUEST, "configure");
                            body.put("audio", true);
                            //body.put("video", true);//--------------change------
                            body.put("video", false);
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

        private void registerUsername() {
            if(handle != null) {
                JSONObject obj = new JSONObject();
                JSONObject msg = new JSONObject();
                try
                {
                    obj.put(REQUEST, "join");
                    obj.put("room", roomid);
                    obj.put("ptype", "publisher");//subscriber   publisher
                    //obj.put("feed","" );//如果为subscriber，则必须指定publisherID
                    obj.put("display", user_name);
                    msg.put(MESSAGE, obj);
                }
                catch(Exception ex)
                {

                }
                handle.sendMessage(new PluginHandleSendMessageCallbacks(msg));
            }
        }

        private void newRemoteFeed(BigInteger id) { //todo attach the plugin as a listener
            //VideoRenderer.Callbacks myrenderer;
            //if(!remoteRenderers.containsKey(id))
            //{
            //    if(availableRemoteRenderers.isEmpty())
            //    {
            //        //TODO no more space
            //        return;
            //    }
            //    remoteRenderers.put(id, availableRemoteRenderers.pop());
            //}
            //myrenderer = remoteRenderers.get(id);
            janusServer.Attach(new ListenerAttachCallbacks(id));
        }

        @Override
        public void success(JanusPluginHandle pluginHandle) {
            handle = pluginHandle;
            registerUsername();
        }

        @Override
        public void onMessage(JSONObject msg, JSONObject jsepLocal) {
            try
            {
                //String event = msg.getString("videoroom");//--------------change-----
                //String event = msg.getString("audioroom");//--------------change-----
                String event = msg.getString("audiobridge");
                if(event.equals("joined")) {
                    myid = new BigInteger(msg.getString("id"));
                    publishOwnFeed();
                    if(msg.has(PUBLISHERS)){
                        JSONArray pubs = msg.getJSONArray(PUBLISHERS);
                        for(int i = 0; i < pubs.length(); i++) {
                            JSONObject pub = pubs.getJSONObject(i);
                            BigInteger tehId = new BigInteger(pub.getString("id"));
                            newRemoteFeed(tehId);
                        }
                    }
                } else if(event.equals("destroyed")) {

                } else if(event.equals("event")) {
                    if(msg.has(PUBLISHERS)){
                        JSONArray pubs = msg.getJSONArray(PUBLISHERS);
                        for(int i = 0; i < pubs.length(); i++) {
                            JSONObject pub = pubs.getJSONObject(i);
                            newRemoteFeed(new BigInteger(pub.getString("id")));
                        }
                    } else if(msg.has("leaving")) {

                    } else if(msg.has("unpublished")) {

                    } else {
                        //todo error
                    }
                }
                if(jsepLocal != null) {
                    handle.handleRemoteJsep(new PluginHandleWebRTCCallbacks(null, jsepLocal, false));
                }
            }
            catch (Exception ex)
            {

            }
        }

        @Override
        public void onLocalStream(MediaStream stream) {
            //stream.videoTracks.get(0).addRenderer(new VideoRenderer(localRender));//----------change
        }

        @Override
        public void onRemoteStream(MediaStream stream) {

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
            //return JanusSupportedPluginPackages.JANUS_VIDEO_ROOM;//--------------change-----
            return JanusSupportedPluginPackages.JANUS_AUDIO_BRIDGE;
        }

        @Override
        public void onCallbackError(String error) {

        }

        @Override
        public void onDetached() {

        }
    }

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

    public boolean initializeMediaContext(Context context, boolean audio, boolean video, boolean videoHwAcceleration, EGLContext eglContext){
        janusServer = new JanusServer(new JanusGlobalCallbacks());
        return janusServer.initializeMediaContext(context, audio, video, videoHwAcceleration, eglContext);
    }

    public void Start() {
        janusServer.Connect();
    }


    //发送publisher 信令
    public void sendPublisher(){
        try
        {
            JSONObject msg = new JSONObject();
            JSONObject body = new JSONObject();
            body.put(REQUEST, "publish");
            body.put("audio", true);
            //body.put("video", true);//--------------change------
            body.put("video", false);
            body.put("data", false);
//            body.put("audiocodec", user_name);
//            body.put("videocodec", user_name);
//            body.put("bitrate", user_name);
//            body.put("record", user_name);
//            body.put("filename", user_name);
            body.put("display", user_name);
            msg.put(MESSAGE, body);
            handle.sendMessage(new PluginHandleSendMessageCallbacks(msg));
        }catch (Exception ex) {

        }
    }

    public void sendListParticipants(){
        try
        {
            JSONObject msg = new JSONObject();
            JSONObject body = new JSONObject();
            body.put(REQUEST, "listparticipants");
            body.put("room", 1234);
            msg.put(MESSAGE, body);
            handle.sendMessage(new PluginHandleSendMessageCallbacks(msg));
        }catch (Exception ex) {

        }
    }

    public void sendUnPublish(){
        try
        {
            JSONObject msg = new JSONObject();
            JSONObject body = new JSONObject();
            body.put(REQUEST, "unpublish");
            msg.put(MESSAGE, body);
            handle.sendMessage(new PluginHandleSendMessageCallbacks(msg));
        }catch (Exception ex) {

        }
    }

    public void sendTalk(final MyControlCallBack myControlCallBack, int id){
        try
        {
            final JSONObject msg = new JSONObject();
            JSONObject body = new JSONObject();
            body.put(REQUEST, "talk");
            body.put("room", roomid);
            body.put("secret", "adminpwd");
            body.put("id", id);
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
    };

    public void sendUnTalk(final MyControlCallBack myControlCallBack, int id){
        try
        {
            final JSONObject msg = new JSONObject();
            JSONObject body = new JSONObject();
            body.put(REQUEST, "untalk");
            body.put("room", roomid);
            body.put("secret", "adminpwd");
            body.put("id", id);
//            body.put("id", new BigInteger("4188436743763777"));
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
    };

}
