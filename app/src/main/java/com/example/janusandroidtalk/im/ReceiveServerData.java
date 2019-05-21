package com.example.janusandroidtalk.im;

import android.content.Intent;
import android.util.Log;

import com.example.janusandroidtalk.MyApplication;
import com.example.janusandroidtalk.bean.UserBean;
import com.example.janusandroidtalk.grpcconnectionmanager.GrpcConnectionManager;
import com.example.janusandroidtalk.im.model.ChatMessage;
import com.example.janusandroidtalk.im.model.DefaultUser;
import com.example.janusandroidtalk.im.network.DownloadManager;
import com.example.janusandroidtalk.im.record.ChatDBManager;
import com.example.janusandroidtalk.im.utils.MediaUtil;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import cn.jiguang.imui.commons.models.IMessage;
import io.grpc.stub.StreamObserver;
import talk_cloud.TalkCloudApp;

/**
 * 接收消息的线程
 */
public class ReceiveServerData extends Thread {
    private static final String TAG = "ReceiveServerData";
    private ChatDBManager mDbManager;
    final String destFileDir = MyApplication.getContext().getFilesDir() + "/media";

    private final int IM_MSG_TYPE_SINGLE = 1;
    private final int IM_MSG_TYPE_GROUP = 2;

    private final int MSG_TYPE_TEXT = 1;
    private final int MSG_TYPE_IMAGE = 2;
    private final int MSG_TYPE_VOICE = 3;
    private final int MSG_TYPE_VIDEO = 4;

    private final int DATA_TYPE_OFFLINE = 2;
    private final int DATA_TYPE_ONLINE = 3;
    private final int DATA_TYPE_HEARTBEAT = 4;

    // TODO Simple schedule simple thread to update online state
    private ScheduledExecutorService onlineStateUpdater = Executors.newScheduledThreadPool(1);

    private void initChatMessage(ChatMessage chatMessage, TalkCloudApp.ImMsgReqData imMsgReqData) {
        /**
         * receiver id :
         * receiver name :
         * send time :
         * sender info :
         * message type :
         * text :
         * resource path :
         * duration :
         * MessageStatus :
         */
        chatMessage.setReceiveId(imMsgReqData.getReceiverId());
        chatMessage.setReceiveName(imMsgReqData.getSenderName());
        chatMessage.setTimeString(imMsgReqData.getSendTime());
        chatMessage.setUserInfo(new DefaultUser(imMsgReqData.getId() + "", imMsgReqData.getSenderName(), ""));
        chatMessage.setTimestamp(DateTimeFormat(chatMessage.getTimeString()) / 1000);
        Log.d("ReceiveServerData", "this is transfer dateTimeStr " + chatMessage.getTimeString() + " to timestamp " + chatMessage.getTimestamp());
        int msgType = imMsgReqData.getMsgType();
        Log.d("ReceiveServerData", "this is streamObserver onNext and msgType = " + msgType);

        if (msgType == MSG_TYPE_TEXT) {//接收文字
            chatMessage.setType(IMessage.MessageType.RECEIVE_TEXT.ordinal());
            chatMessage.setText(imMsgReqData.getResourcePath());
        }

        if (msgType == MSG_TYPE_IMAGE || msgType == MSG_TYPE_VOICE || msgType == MSG_TYPE_VIDEO) {
            if (msgType == MSG_TYPE_IMAGE)//接收图片
                chatMessage.setType(IMessage.MessageType.RECEIVE_IMAGE.ordinal());
            if (msgType == MSG_TYPE_VOICE)//接收语音
                chatMessage.setType(IMessage.MessageType.RECEIVE_VOICE.ordinal());
            if (msgType == MSG_TYPE_VIDEO)//接收视频
                chatMessage.setType(IMessage.MessageType.RECEIVE_VIDEO.ordinal());

            String[] str = imMsgReqData.getResourcePath().split("/");
            Log.d(TAG, TAG + " this is StreamObserver receive text  destFileDir="+destFileDir);
            chatMessage.setMediaFilePath(destFileDir + "/" + str[str.length - 1]);
        }
    }

    private void mediaFileHandler(ChatMessage chatMessage, TalkCloudApp.ImMsgReqData imMsgReqData) {
        String[] str = imMsgReqData.getResourcePath().split("/");
        //图片、语音、视频
        DownloadManager.getInstance().downloadFile(imMsgReqData.getResourcePath(), str[str.length - 1], chatMessage, new DownloadManager.OnDownloadListener() {
            @Override
            public void onDownloadSuccess(ChatMessage msg) {
                //下载成功
                msg.setDuration(MediaUtil.getDuration(new File(msg.getMediaFilePath())));
                long id = mDbManager.addRecord(msg);
                sendReceiverBroadcast(id);
            }

            @Override
            public void onDownloading(int progress) {
                //下载中
            }

            @Override
            public void onDownloadFailed(ChatMessage msg) {
                //下载失败
            }
        });
    }

    private void singleOnlineImMsg(TalkCloudApp.ImMsgReqData imMsgReqData) {
        Log.d("ReceiveServerData", "This is singleOnlineImMsg receive single online message " + imMsgReqData.toString());

        //接收到自己发送的消息则不做处理
        if (imMsgReqData.getId() != UserBean.getUserBean().getUserId()) {
            ChatMessage chatMessage = new ChatMessage();
            initChatMessage(chatMessage, imMsgReqData);

            if (imMsgReqData.getMsgType() == MSG_TYPE_TEXT) {
                long id = mDbManager.addRecord(chatMessage);
                sendReceiverBroadcast(id);
            }
            else {
                mediaFileHandler(chatMessage, imMsgReqData);
            }
            Log.d("ReceiveServerData", "This is handleOnlineMessage receive single online message " + chatMessage.toString());
        }
    }

    private void groupOnlineImMsg(TalkCloudApp.ImMsgReqData imMsgReqData) {
        //接收到自己发送的消息则不做处理
        if (imMsgReqData.getId() != UserBean.getUserBean().getUserId()) {
            ChatMessage chatMessage = new ChatMessage();
            initChatMessage(chatMessage, imMsgReqData);
            if (imMsgReqData.getMsgType() == MSG_TYPE_TEXT) {
                long id = mDbManager.addRecord(chatMessage);
                sendReceiverBroadcast(id);
            }
            else {
                mediaFileHandler(chatMessage, imMsgReqData);
            }
            Log.d("ReceiveServerData", "This is handleOnlineMessage receive group online message " + chatMessage.toString());
        }
    }

    public void handleOnlineMessage(TalkCloudApp.StreamResponse value) {
        // SingleChat online message
        if (value.getImMsgData().getReceiverType() == IM_MSG_TYPE_SINGLE) {
            singleOnlineImMsg(value.getImMsgData());
        }

        // GroupChat online message
        if (value.getImMsgData().getReceiverType() == IM_MSG_TYPE_GROUP) {
//            for (int i = 0; i < UserBean.getUserBean().getUserGroupBeanArrayList().size(); i++) {
//                if (UserBean.getUserBean().getUserGroupBeanArrayList().get(i).getUserGroupId() == value.getImMsgData().getReceiverId()) {
                    groupOnlineImMsg(value.getImMsgData());
//                }
//            }
        }
    }

    private void singleOfflineImMsg(List<TalkCloudApp.OfflineImMsg> offlineImMsgsList) {
        Log.d("ReceiveServerData", "This is handleOfflineMessage receive single offlineImMsgsList size = " + offlineImMsgsList.size());
        for (int i = 0; i < offlineImMsgsList.size(); i++) {
            List<TalkCloudApp.ImMsgReqData> imMsgReqDataList = offlineImMsgsList.get(i).getImMsgDataList();
            Log.d("ReceiveServerData", "This is handleOfflineMessage receive single offlineImMsgsList " + i + " size = " + imMsgReqDataList.size());
            for (int j = 0; j < imMsgReqDataList.size(); j++) {
                if (imMsgReqDataList.get(j).getReceiverType() == IM_MSG_TYPE_SINGLE) {
                    TalkCloudApp.ImMsgReqData imMsgReqData = imMsgReqDataList.get(j);
                    ChatMessage chatMessage = new ChatMessage();
                    initChatMessage(chatMessage, imMsgReqData);

                    mDbManager.addRecord(chatMessage);
                    Log.d("ReceiveServerData", "This is handleOfflineMessage receive single offline message " + chatMessage.toString());
                }
            }
        }
    }

    private void groupOfflineImMsg(List<TalkCloudApp.OfflineImMsg> offlineImMsgsList) {
        Log.d("ReceiveServerData", "This is handleOfflineMessage receive group offlineImMsgsList size = " + offlineImMsgsList.size());
        for (int i = 0; i < offlineImMsgsList.size(); i++) {
            List<talk_cloud.TalkCloudApp.ImMsgReqData> imMsgReqDataList = offlineImMsgsList.get(i).getImMsgDataList();
            for (int j = 0; j < imMsgReqDataList.size(); j++) {
                TalkCloudApp.ImMsgReqData groupData = imMsgReqDataList.get(j);
                ChatMessage chatMessage = new ChatMessage();
                initChatMessage(chatMessage, groupData);

                mDbManager.addRecord(chatMessage);
                Log.d("ReceiveServerData", "This is handleOfflineMessage receive group offline message " + chatMessage.toString());
            }
        }
    }

    public void handleOfflineMessage(TalkCloudApp.StreamResponse value) {
        // Single offline im message
        List<TalkCloudApp.OfflineImMsg> singleOfflineImMsgList = value.getOfflineImMsgResp().getOfflineSingleImMsgsList();
        singleOfflineImMsg(singleOfflineImMsgList);

        // Group offline im message
        List<TalkCloudApp.OfflineImMsg> groupOfflineMsgList = value.getOfflineImMsgResp().getOfflineGroupImMsgsList();
        groupOfflineImMsg(groupOfflineMsgList);
    }

    @Override
    public void run() {
        super.run();
        Log.d(TAG, TAG + " this is testing 0000000000000");
        mDbManager = new ChatDBManager(MyApplication.getContext());

//        StreamObserver<TalkCloudApp.StreamRequest> offlineReq = new StreamObserver<TalkCloudApp.StreamRequest>() {
//            @Override
//            public void onNext(TalkCloudApp.StreamRequest value) {
//
//            }
//
//            @Override
//            public void onError(Throwable t) {
//
//            }
//
//            @Override
//            public void onCompleted() {
//
//            }
//        };

        StreamObserver<TalkCloudApp.StreamResponse> response = new StreamObserver<TalkCloudApp.StreamResponse>() {
            @Override
            public void onNext(TalkCloudApp.StreamResponse value) {
                //接收服务器下发的消息
                int type = value.getDataType();

                if (type == DATA_TYPE_ONLINE) {//3在线消息
                    Log.d("ReceiveServerData", "This is StreamObserver onNext handleOnlineMessage ");
                    handleOnlineMessage(value);
                } else if (type == DATA_TYPE_OFFLINE) {//2离线消息
                    Log.d("ReceiveServerData", "This is StreamObserver onNext handleOfflineMessage ");
                    handleOfflineMessage(value);
                }
            }

            @Override
            public void onError(Throwable t) {
                Log.d(TAG, TAG + " this is StreamObserver onError and " + t.getMessage());
                restart();// TODO reconnect by this？
            }

            @Override
            public void onCompleted() {
                Log.d(TAG, TAG + " this is StreamObserver onCompleted");
                restart();// TODO reconnect by this？
            }
        };

        StreamObserver<TalkCloudApp.StreamRequest> request = GrpcConnectionManager.getInstance().getStub().dataPublish(response);

        Log.d(TAG, TAG + " this is testing 11111111111");
        try {
            Log.d(TAG, TAG + " this is testing 2222222222222222222 and uerid=" + UserBean.getUserBean().getUserId());
            TalkCloudApp.StreamRequest streamRequest = TalkCloudApp.StreamRequest.newBuilder().setUid(UserBean.getUserBean().getUserId()).setDataType(DATA_TYPE_OFFLINE).build();
            request.onNext(streamRequest);
            //request.onCompleted(); //TODO Could not onCompleted, why?

            TalkCloudApp.StreamRequest keepAliveReq = TalkCloudApp.StreamRequest.newBuilder().setUid(UserBean.getUserBean().getUserId()).setDataType(DATA_TYPE_HEARTBEAT).build();
            onlineStateUpdater.scheduleAtFixedRate(() -> {request.onNext(keepAliveReq); Log.d(TAG, TAG + " this is testing keepalive");}, 0, 3*1000, TimeUnit.MILLISECONDS);

            Log.d(TAG, TAG + " this is testing 3333333333");
        } catch (Exception e) {
            Log.d(TAG, TAG + " this is testing 444444444");
            request.onError(e);
        }
    }

    //发送广播通知接收到消息
    private void sendReceiverBroadcast(long id) {
        Intent intent = new Intent();
        intent.setAction("com_jimi_chat_updatedata");
        intent.putExtra("id", id);
        MyApplication.getContext().sendBroadcast(intent);
    }

    //重新连接服务器
    private void restart(){
        Log.d(TAG, TAG + " this is StreamObserver and restart --------------start");
        new ReceiveServerData().start();
        try {
            sleep(5*1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 时间字符串转换为时间戳
     * @param dateTimeStr yyyy-MM-dd HH:mm:ss
     * @return  timestamp long
     */
    private long DateTimeFormat(String dateTimeStr) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            long timestamp = format.parse(dateTimeStr).getTime();
            return timestamp;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}
