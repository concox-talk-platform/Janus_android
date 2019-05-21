package com.example.janusandroidtalk.im.model;

import java.util.HashMap;
import java.util.UUID;
import cn.jiguang.imui.commons.models.IMessage;
import cn.jiguang.imui.commons.models.IUser;


public class ChatMessage implements IMessage {

    private long id;//message的id
    private String text;//文字
    private String timeString;//时间
    private int type;//类型
    private IUser user;//用户
    private String mediaFilePath;//资源路径
    private long duration;//持续时间,语音、视频的时长
    private String progress;
    private MessageStatus mMsgStatus = MessageStatus.CREATED;//发送的状态
    private int receiveId;//接收者id
    private String receiveName;//接收者名字
    private long timestamp; //存储时间戳
    private int messageType; //1文本2媒体
    private int receiverType; //1个人2群组

    public int getReceiverType() {
        return receiverType;
    }

    public void setReceiverType(int receiverType) {
        this.receiverType = receiverType;
    }

    public int getMessageType() {
        return messageType;
    }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public ChatMessage() {
        this.id = UUID.randomUUID().getLeastSignificantBits();
    }

    @Override
    public String getMsgId() {
        return String.valueOf(id);
    }

    public long getId() {
        return this.id;
    }

    @Override
    public IUser getFromUser() {
        if (user == null) {
            return new DefaultUser("0", " ", null);
        }
        return user;
    }

    public void setUserInfo(IUser user) {
        this.user = user;
    }

    public void setMediaFilePath(String path) {
        this.mediaFilePath = path;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    @Override
    public long getDuration() {
        return duration;
    }

    public void setProgress(String progress) {
        this.progress = progress;
    }

    @Override
    public String getProgress() {
        return progress;
    }

    @Override
    public HashMap<String, String> getExtras() {
        return null;
    }

    public void setTimeString(String timeString) {
        this.timeString = timeString;
    }

    @Override
    public String getTimeString() {
        return timeString;
    }

    public void setType(int type) {
        /*if (type >= 0 && type <= 12) {
            throw new IllegalArgumentException("Message type should not take the value between 0 and 12");
        }*/
        this.type = type;
    }

    @Override
    public int getType() {
        return type;
    }

    /**
     * Set Message status. After sending Message, change the status so that the progress bar will dismiss.
     * @param messageStatus {@link cn.jiguang.imui.commons.models.IMessage.MessageStatus}
     */
    public void setMessageStatus(MessageStatus messageStatus) {
        this.mMsgStatus = messageStatus;
    }

    @Override
    public MessageStatus getMessageStatus() {
        return this.mMsgStatus;
    }

    @Override
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String getMediaFilePath() {
        return mediaFilePath;
    }

    public int getReceiveId() {
        return receiveId;
    }

    public void setReceiveId(int receiveId) {
        this.receiveId = receiveId;
    }

    public String getReceiveName() {
        return receiveName;
    }

    public void setReceiveName(String receiveName) {
        this.receiveName = receiveName;
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "id=" + id +
                ", text='" + text + '\'' +
                ", timeString='" + timeString + '\'' +
                ", type=" + type +
                ", message type=" + messageType +
                ", user=" + user +
                ", mediaFilePath='" + mediaFilePath + '\'' +
                ", duration=" + duration +
                ", progress='" + progress + '\'' +
                ", mMsgStatus=" + mMsgStatus +
                ", receiveId=" + receiveId +
                ", receiverType=" + receiverType +
                ", record timestamp=" + timestamp +
                '}';
    }
}