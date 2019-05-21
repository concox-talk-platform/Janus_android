package com.example.janusandroidtalk.im;

import android.os.AsyncTask;
import android.util.Log;

import com.example.janusandroidtalk.grpcconnectionmanager.GrpcConnectionManager;
import com.example.janusandroidtalk.im.model.ChatMessage;
import com.example.janusandroidtalk.tools.AppTools;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import cn.jiguang.imui.commons.models.IMessage;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import talk_cloud.TalkCloudApp;

public class SendControll {
    public static final int MSG_TYPE_TEXT = 1;
    public static final int MSG_TYPE_MEDIA = 2;

    public static final int RECEIVER_TYPE_SINGLE = 1;
    public static final int RECEIVER_TYPE_GROUP = 2;

    /**
     * 上传媒体文件的路径
     */
    public static final String UPLOAD_URL = "https://test.jimilab.com:10000/upload";

    private static OnSendCallbackListener onSendCallbackListener;
    public void setSendCallbackListener(OnSendCallbackListener listener){
        onSendCallbackListener = listener;
    }
    /**
     * 回调通知发送者发送的结果
     */
    public interface OnSendCallbackListener{
        void OnTextMsg(boolean flag, String msgId);
        void OnMediaMsg(boolean flag, String msgId);
    }

    public static void sendMessage(ChatMessage chatMessage) {
        Log.d("SendControll", "this is before sending chatMessage " + chatMessage.toString());
        if (MSG_TYPE_TEXT == chatMessage.getMessageType()) {
            new ChatTextTask().execute(chatMessage);
        }

        if (MSG_TYPE_MEDIA == chatMessage.getMessageType()) {
            new ChatMediaTask().execute(chatMessage);
        }
    }

    //文字上传
    public static class ChatTextTask extends AsyncTask<ChatMessage, Void, TalkCloudApp.ImMsgRespData> {
        private TalkCloudApp.ImMsgRespData reply = null;
        @Override
        protected TalkCloudApp.ImMsgRespData doInBackground(ChatMessage... chatMessages) {
            ChatMessage message = chatMessages[0];
            int userId = Integer.valueOf(message.getFromUser().getId());
            try{
                Log.d("SendControll"," this is ChatTextTask doInBackground receive=" + message.getReceiveId()+" msgid=" + message.getMsgId());
                TalkCloudApp.ImMsgReqData data = TalkCloudApp.ImMsgReqData.newBuilder()
                        .setId(userId)
                        .setSenderName(message.getFromUser().getDisplayName())
                        .setReceiverId(message.getReceiveId())
//                        .setReceiverName(message.getReceiveName()) //FIXME 如果是群消息就没有具体的接受者，如果是单对单的消息是否需要增加相应的接受者?
                        .setReceiverType(message.getReceiverType()) //TODO 1是个人，2是群
                        .setMsgType(message.getMessageType()) //TODO 1是文字
                        .setMsgCode(message.getMsgId())
                        .setResourcePath(message.getText())
                        .setSendTime(message.getTimeString())
                        .build();

                reply = GrpcConnectionManager.getInstance().getBlockingStub().imMessagePublish(data);
                return reply;
            }catch (Exception e){
                if(onSendCallbackListener != null)
                    onSendCallbackListener.OnTextMsg(false, message.getMsgId());
                return reply;
            }
        }
        @Override
        protected void onPostExecute(TalkCloudApp.ImMsgRespData result) {
            super.onPostExecute(result);

            if(result != null){
                Log.d("SendControll"," this is text onPostExecute code = "+result.getResult().getCode());
                if(result.getResult().getCode() == 200 && onSendCallbackListener != null){
                    onSendCallbackListener.OnTextMsg(true, result.getMsgCode());
                }else{
                    onSendCallbackListener.OnTextMsg(false,"");
                }
            }else{
                if(onSendCallbackListener != null)
                    onSendCallbackListener.OnTextMsg(false,"");
                Log.d("SendControll"," this is text onPostExecute and result == null");
            }
        }
    }

    //图片、语音、视频上传
    public static class ChatMediaTask extends AsyncTask<ChatMessage, Void, Response> {
        private ChatMessage message;
        @Override
        protected Response doInBackground(ChatMessage... chatMessages) {
            message = chatMessages[0];
            Log.d("SendControll"," this is uploadImage path = " + message.getMediaFilePath());
            String[] str = message.getMediaFilePath().split("/");
            String fileName = System.currentTimeMillis()+"_"+str[str.length-1];
            Log.d("SendControll"," this is uploadImage fileName = " + fileName);
            OkHttpClient client = new OkHttpClient();
            try {
                RequestBody body = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file",fileName,
                                RequestBody.create(MediaType.parse("multipart/form-data"), new File(message.getMediaFilePath())))
//                                RequestBody.create(MediaType.parse("application/octet-stream"), new File(message.getMediaFilePath())))
                        .addFormDataPart("id",message.getFromUser().getId())
                        .addFormDataPart("SenderName",message.getFromUser().getDisplayName())
                        .addFormDataPart("ReceiverType",message.getReceiverType() + "") //TODO 1是个人，2是群
                        .addFormDataPart("ReceiverId",message.getReceiveId() + "")
                        .addFormDataPart("ReceiverName",message.getReceiveName())
                        .addFormDataPart("SendTime",message.getTimeString())
                        .build();

                Request request = new Request.Builder()
                        .header("Authorization", "Client-ID " + UUID.randomUUID())
                        .url(UPLOAD_URL)
                        .post(body)
                        .build();
                Response response = client.newCall(request).execute();
                Log.d("SendControll"," this is uploadImage response = " + response.isSuccessful());
                Log.d("SendControll"," this is uploadImage body = " + response.body());
                if(onSendCallbackListener != null)
                    onSendCallbackListener.OnMediaMsg(response.isSuccessful(), message.getMsgId());
                return response;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Response result) {
            super.onPostExecute(result);
            if(result!=null){
                Log.d("SendControll"," this is onPostExecute and code = " + result.code() + " body = " + result.body());
                if(result.code() != 201 && onSendCallbackListener != null){
                    onSendCallbackListener.OnMediaMsg(false, message.getMsgId());
                }
            }else{
                if(onSendCallbackListener != null)
                    onSendCallbackListener.OnMediaMsg(false, message.getMsgId());
                Log.d("SendControll"," this is onPostExecute and result==null");
            }
        }
    }
}
