package com.example.janusandroidtalk.im;

import android.content.Intent;
import android.util.Log;

import com.example.janusandroidtalk.MyApplication;
import com.example.janusandroidtalk.bean.UserBean;
import com.example.janusandroidtalk.im.model.ChatMessage;
import com.example.janusandroidtalk.im.model.DefaultUser;
import com.example.janusandroidtalk.im.network.DownloadManager;
import com.example.janusandroidtalk.im.record.ChatDBManager;
import com.example.janusandroidtalk.im.utils.MediaUtil;
import com.example.janusandroidtalk.tools.AppTools;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import cn.jiguang.imui.commons.models.IMessage;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import talk_cloud.TalkCloudApp;
import talk_cloud.TalkCloudGrpc;

/**
 * 接收消息的线程
 */
public class ReceiveServerData extends Thread {
    private static final String TAG = "ReceiveServerData";
    private TalkCloudGrpc.TalkCloudStub stub;
    private ChatDBManager mDbManager;
    final String destFileDir = MyApplication.getContext().getFilesDir() + "/media";

    @Override
    public void run() {
        super.run();
        Log.d(TAG, TAG + " this is testing 0000000000000");
        final ManagedChannel channel = ManagedChannelBuilder.forAddress(AppTools.host, AppTools.port).usePlaintext().build();
        stub = TalkCloudGrpc.newStub(channel);
        mDbManager = new ChatDBManager(MyApplication.getContext());
        StreamObserver<TalkCloudApp.StreamResponse> response = new StreamObserver<TalkCloudApp.StreamResponse>() {
            @Override
            public void onNext(TalkCloudApp.StreamResponse value) {
                //接收服务器下发的消息
                Log.d(TAG, TAG + " this is StreamObserver onNext");
                long result = value.getRes().getCode();
                int type = value.getDataType();
                boolean isInGroup = false;
                Log.d(TAG, TAG + " this is StreamObserver onNext and result = " + result + " type = " + type);
                Log.d(TAG, TAG + " this is StreamObserver onNext and getMsg = " + value.getRes().getMsg());
                if (type == 3) {//在线消息
                    //判断下发的消息是否有当前用户的群组
                    for (int i = 0; i < UserBean.getUserBean().getUserGroupBeanArrayList().size(); i++) {
                        if (UserBean.getUserBean().getUserGroupBeanArrayList().get(i).getUserGroupId() == value.getImMsgData().getReceiverId()) {
                            isInGroup = true;
                        }
                    }
                    Log.d(TAG, TAG + " this is StreamObserver onNext and isInGroup = " + isInGroup);
                    Log.d(TAG, TAG + " this is StreamObserver onNext and id = " + value.getImMsgData().getId());
                    Log.d(TAG, TAG + " this is StreamObserver onNext and userid = " + UserBean.getUserBean().getUserId());
                    if (isInGroup && value.getImMsgData().getId() != UserBean.getUserBean().getUserId()) {//接收到自己发送的消息则不做处理
                        ChatMessage message = new ChatMessage();
                        message.setReceiveId(value.getImMsgData().getReceiverId());
                        message.setUserInfo(new DefaultUser(value.getImMsgData().getId() + "", value.getImMsgData().getSenderName(), ""));
                        //message.setTimeString(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
                        message.setTimeString(value.getImMsgData().getSendTime());
                        int msgType = value.getImMsgData().getMsgType();
                        Log.d(TAG, TAG + " this is StreamObserver onNext and msgType = " + msgType);
                        if (mDbManager != null) {
                            if (msgType == 1) {//接收文字
                                message.setType(IMessage.MessageType.RECEIVE_TEXT.ordinal());
                                message.setText(value.getImMsgData().getResourcePath());
                                long id = mDbManager.addRecord(message);
                                sendReceiverBroadcast(id);
                            }
                            if (msgType == 2 || msgType == 3 || msgType == 4) {
                                if (msgType == 2)//接收图片
                                    message.setType(IMessage.MessageType.RECEIVE_IMAGE.ordinal());
                                if (msgType == 3)//接收语音
                                    message.setType(IMessage.MessageType.RECEIVE_VOICE.ordinal());
                                if (msgType == 4)//接收视频
                                    message.setType(IMessage.MessageType.RECEIVE_VIDEO.ordinal());
                                String[] str = value.getImMsgData().getResourcePath().split("/");
                                Log.d(TAG, TAG + " this is StreamObserver receive text  destFileDir="+destFileDir);
                                message.setMediaFilePath(destFileDir + "/" + str[str.length - 1]);
                                //long id = mDbManager.addRecord(message);
                                //download(value.getImMsgData().getResourcePath(),str[str.length-1],message);
                                //图片、语音、视频
                                DownloadManager.getInstance().downloadFile(value.getImMsgData().getResourcePath(), str[str.length - 1], message, new DownloadManager.OnDownloadListener() {
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
                                        long id = mDbManager.addRecord(msg);
                                        sendReceiverBroadcast(id);
                                    }
                                });
                            }
                        }
                    }
                } else if (type == 2) {//离线消息
                    List<TalkCloudApp.OfflineImMsg> list = value.getOfflineImMsgResp().getOfflineGroupImMsgsList();
                    Log.d(TAG, TAG + " this is StreamObserver type ==2 and list = " + list.size());
                    for (int i = 0; i < list.size(); i++) {
                        int groupId = list.get(i).getGroupId();
                        List<talk_cloud.TalkCloudApp.ImMsgReqData> groupList = list.get(i).getImMsgDataList();
                        for (int j = 0; j < groupList.size(); j++) {
                            int id = groupList.get(j).getId();
                            String text = groupList.get(j).getResourcePath();
                            TalkCloudApp.ImMsgReqData groupData = groupList.get(j);
                            ChatMessage chatMessage = new ChatMessage();
                            chatMessage.setUserInfo(new DefaultUser(groupData.getId() + "", groupData.getSenderName(), ""));
                            chatMessage.setReceiveId(groupData.getReceiverId());
                            //groupData.getSendTime();
                            Log.d(TAG, TAG + " this is StreamObserver sendTime = " + groupData.getSendTime() + " msgtype=" + groupData.getMsgType());
                            chatMessage.setTimeString(groupData.getSendTime());
                            if (groupData.getMsgType() == 1) {
                                chatMessage.setType(IMessage.MessageType.RECEIVE_TEXT.ordinal());
                                chatMessage.setText(groupData.getResourcePath());
                                long resultId = mDbManager.addRecord(chatMessage);

                            }
                            if (groupData.getMsgType() == 2 || groupData.getMsgType() == 3 || groupData.getMsgType() == 4) {
                                if (groupData.getMsgType() == 2)
                                    chatMessage.setType(IMessage.MessageType.RECEIVE_IMAGE.ordinal());
                                if (groupData.getMsgType() == 3)
                                    chatMessage.setType(IMessage.MessageType.RECEIVE_VOICE.ordinal());
                                if (groupData.getMsgType() == 4)
                                    chatMessage.setType(IMessage.MessageType.RECEIVE_VIDEO.ordinal());
                                String[] str = groupData.getResourcePath().split("/");
                                chatMessage.setMediaFilePath(destFileDir + "/" + str[str.length - 1]);
                                //long id = mDbManager.addRecord(message);
                                //download(groupData.getResourcePath(),str[str.length-1],chatMessage);
                                //图片、语音、视频下载
                                DownloadManager.getInstance().downloadFile(groupData.getResourcePath(), str[str.length - 1], chatMessage, new DownloadManager.OnDownloadListener() {
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
                                        long id = mDbManager.addRecord(msg);
                                        sendReceiverBroadcast(id);
                                    }
                                });
                            }
                        }
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                Log.d(TAG, TAG + " this is StreamObserver onError and " + t.getMessage());
                close(channel);
                restart();
            }

            @Override
            public void onCompleted() {
                Log.d(TAG, TAG + " this is StreamObserver onCompleted");
                close(channel);
                restart();
            }
        };
        StreamObserver<TalkCloudApp.StreamRequest> request = stub.dataPublish(response);
        Log.d(TAG, TAG + " this is testing 11111111111");
        try {
            TalkCloudApp.ImMsgReqData data = TalkCloudApp.ImMsgReqData.newBuilder().setId(1).setReceiverType(1).setReceiverId(1)
                    .setResourcePath("").setMsgType(1).build();
            Log.d(TAG, TAG + " this is testing 2222222222222222222 and uerid=" + UserBean.getUserBean().getUserId());
            TalkCloudApp.StreamRequest value = TalkCloudApp.StreamRequest.newBuilder().setUid(UserBean.getUserBean().getUserId()).setDataType(2).build();
            request.onNext(value);
            request.onCompleted();
        } catch (Exception e) {
            Log.d(TAG, TAG + " this is testing 33333333333");
            request.onError(e);
        }
    }

    private void close(ManagedChannel channel) {
        try {
            channel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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
    }
}
