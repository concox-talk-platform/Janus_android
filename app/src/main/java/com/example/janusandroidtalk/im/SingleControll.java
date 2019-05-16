package com.example.janusandroidtalk.im;

import android.os.AsyncTask;
import android.util.Log;

import com.example.janusandroidtalk.grpcconnectionmanager.GrpcConnectionManager;
import com.example.janusandroidtalk.im.model.ChatMessage;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import talk_cloud.TalkCloudApp;

public class SingleControll {
    private final static String TAG = "SingleControll";// TODO Don't do like this
    private static OnSingleInfoListener gListener;
    private static String UPLOAD_URL = "http://113.105.153.240:8888/upload";

    public void setOnSingleInfoListener(OnSingleInfoListener listener){
        gListener = listener;
    }

    /**
     * 回调通知发送者发送的结果
     */
    public interface OnSingleInfoListener{
        void OnTextMsg(boolean b,String msgId);
        void OnMediaMsg(boolean b,String msgId);
    }

    //文字上传
    public static class ChatTextTask extends AsyncTask<ChatMessage,Void, TalkCloudApp.ImMsgRespData>{
        private TalkCloudApp.ImMsgRespData reply = null;
        @Override
        protected TalkCloudApp.ImMsgRespData doInBackground(ChatMessage... chatMessages) {
            ChatMessage message = chatMessages[0];
            int userId = Integer.valueOf(message.getFromUser().getId());
            try{
                Log.d(TAG,TAG+" this is ChatTextTask doInBackground receive="+message.getReceiveId()+" msgid="+message.getMsgId());
                TalkCloudApp.ImMsgReqData data = TalkCloudApp.ImMsgReqData.newBuilder()
                        .setId(userId)
                        .setSenderName(message.getFromUser().getDisplayName())
                        .setReceiverId(message.getReceiveId())
//                        .setReceiverName(message.getReceiveName()) //FIXME 如果是群消息就没有具体的接受者，如果是单对单的消息是否需要增加相应的接受者?
                        .setReceiverType(1) //TODO 1是个人，2是群
                        .setMsgType(1) //TODO 1是文字
                        .setMsgCode(message.getMsgId())
                        .setResourcePath(message.getText())
                        .setSendTime(message.getTimeString())
                        .build();

                reply = GrpcConnectionManager.getInstance().getBlockingStub().imMessagePublish(data);
                Log.d(TAG,TAG+" this is ChatTextTask doInBackground 2222 ...");
                return reply;
            }catch (Exception e){
                if(gListener!=null)
                    gListener.OnTextMsg(false,message.getMsgId());
                return reply;
            }
        }
        @Override
        protected void onPostExecute(TalkCloudApp.ImMsgRespData result) {
            super.onPostExecute(result);

            if(result!=null){
                Log.d(TAG,TAG+" this is text onPostExecute code = "+result.getResult().getCode());
                if(result.getResult().getCode()==200&&gListener!=null){
                    gListener.OnTextMsg(true,result.getMsgCode());
                }else{
                    gListener.OnTextMsg(false,"");
                }
            }else{
                if(gListener!=null)
                    gListener.OnTextMsg(false,"");
                Log.d(TAG,TAG+" this is text onPostExecute and result==null");
            }
        }
    }

    //图片、语音、视频上传
    public static class ChatTask extends AsyncTask<ChatMessage, Void, Response> {
        private ChatMessage message;
        @Override
        protected Response doInBackground(ChatMessage... chatMessages) {
            message = chatMessages[0];
            Log.d(TAG,TAG+" this is uploadImage path="+message.getMediaFilePath());
            String[] str = message.getMediaFilePath().split("/");
            String fileName = System.currentTimeMillis()+"_"+str[str.length-1];
            Log.d(TAG,TAG+" this is uploadImage fileName="+fileName);
            OkHttpClient client = new OkHttpClient();
            try {
                RequestBody body = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file",fileName,
                                RequestBody.create(MediaType.parse("multipart/form-data"),new File(message.getMediaFilePath())))
                        .addFormDataPart("id",message.getFromUser().getId())
                        .addFormDataPart("SenderName",message.getFromUser().getDisplayName())
                        .addFormDataPart("ReceiverType",1+"")
                        .addFormDataPart("ReceiverId",message.getReceiveId()+"")
                        .addFormDataPart("ReceiverName",message.getReceiveName())
                        .addFormDataPart("SendTime",message.getTimeString())
                        .build();

                Request request = new Request.Builder()
                        .header("Authorization", "Client-ID " + UUID.randomUUID())
                        .url(UPLOAD_URL)
                        .post(body)
                        .build();
                Response response = client.newCall(request).execute();
                Log.d(TAG,TAG+" this is uploadImage response="+response.isSuccessful());
                Log.d(TAG,TAG+" this is uploadImage body="+response.body());
                if(gListener!=null)
                    gListener.OnMediaMsg(response.isSuccessful(),message.getMsgId());
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
                Log.d(TAG,TAG+" this is onPostExecute and code = "+result.code()+" body="+result.body());
                if(result.code()!=201&&gListener!=null){
                    gListener.OnMediaMsg(false,message.getMsgId());
                }
            }else{
                if(gListener!=null)
                    gListener.OnMediaMsg(false,message.getMsgId());
                Log.d(TAG,TAG+" this is onPostExecute and result==null");
            }
        }
    }
}
