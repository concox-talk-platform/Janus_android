package com.example.janusandroidtalk.im;

import android.os.AsyncTask;
import android.util.Log;

import com.example.janusandroidtalk.im.model.ChatMessage;
import com.example.janusandroidtalk.im.model.GroupInfo;
import com.example.janusandroidtalk.im.model.GroupUser;
import com.example.janusandroidtalk.tools.AppTools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import talk_cloud.TalkCloudApp;
import talk_cloud.TalkCloudGrpc;

public class GroupControll {
    private final static String TAG = "GroupControll";
    private static OnGroupInfoListener gListener;
    GroupInfo info = new GroupInfo();
    ArrayList<GroupUser> groupUsers = new ArrayList<GroupUser>();
    private static String UPLOAD_URL = "http://113.105.153.240:8888/upload";

    /**
     * 获取当前群的用户
     * @param groupId
     * @param userId
     */
    public void getGroupInfo(int groupId,int userId){
       ArrayList list = new ArrayList();
       list.add(groupId);
       list.add(userId);
       new GroupInfoTask().execute(list);
    }

    class GroupInfoTask extends AsyncTask<ArrayList,Void, TalkCloudApp.GetGroupInfoResp>{
        private ManagedChannel channel;
        private TalkCloudApp.GetGroupInfoResp reply = null;
        @Override
        protected TalkCloudApp.GetGroupInfoResp doInBackground(ArrayList... arrayLists) {
            int groupId = (int)arrayLists[0].get(0);
            int userid = (int)arrayLists[0].get(1);
            try{
                channel = ManagedChannelBuilder.forAddress(AppTools.host, AppTools.port).usePlaintext().build();
                TalkCloudGrpc.TalkCloudBlockingStub stub = TalkCloudGrpc.newBlockingStub(channel);
                TalkCloudApp.GetGroupInfoReq resp = TalkCloudApp.GetGroupInfoReq.newBuilder().setGid(groupId).setUid(userid).build();
                reply = stub.getGroupInfo(resp);
                return reply;
            }catch (Exception e){
                return reply;
            }
        }
        @Override
        protected void onPostExecute(TalkCloudApp.GetGroupInfoResp result) {
            super.onPostExecute(result);
            if(result==null)
                return;
            if(result.getRes().getCode()!=200){
                Log.d(TAG,TAG+" getGroupInfo fail and result code = "+result.getRes().getCode());
                return;
            }
            info.setGroupId(result.getGroupInfo().getGid());
            info.setGroupName(result.getGroupInfo().getGroupName());
            List<TalkCloudApp.UserRecord> list = result.getGroupInfo().getUsrListList();
            for (int i=0;i<list.size();i++){
                GroupUser user = new GroupUser();
                user.setUserId(list.get(i).getUid());
                user.setImei(list.get(i).getImei());
                user.setGroupRole(list.get(i).getGrpRole());
                user.setFriend(list.get(i).getIsFriend());
                user.setLockGroupId(list.get(i).getLockGroupId());
                user.setName(list.get(i).getName());
                user.setOnline(list.get(i).getOnline());
                user.setType(list.get(i).getUserType());
                groupUsers.add(user);
                Log.d(TAG,TAG+" group userid="+list.get(i).getUid()+" name="+list.get(i).getName());
            }
            info.setGroupUser(groupUsers);
            if (gListener!=null){
                gListener.OnGroupInfo(info);
            }
        }
    }

    public void setOnGroupInfoListener(OnGroupInfoListener listener){
        gListener = listener;
    }

    public interface OnGroupInfoListener{
        void OnGroupInfo(GroupInfo info);
        void OnTextMsg(boolean b,String msgId);
        void OnMediaMsg(boolean b,String msgId);
    }

    //文字上传
    public static class ChatTextTask extends AsyncTask<ChatMessage,Void, TalkCloudApp.ImMsgRespData>{

        private ManagedChannel channel;
        private TalkCloudApp.ImMsgRespData reply = null;
        @Override
        protected TalkCloudApp.ImMsgRespData doInBackground(ChatMessage... chatMessages) {
            ChatMessage message = chatMessages[0];
            int userId = Integer.valueOf(message.getFromUser().getId());
            try{
                Log.d(TAG,TAG+" this is ChatTextTask doInBackground receive="+message.getReceiveId()+" msgid="+message.getMsgId());
                channel = ManagedChannelBuilder.forAddress(AppTools.host, AppTools.port).usePlaintext().build();
                TalkCloudGrpc.TalkCloudBlockingStub stub = TalkCloudGrpc.newBlockingStub(channel);
                TalkCloudApp.ImMsgReqData data = TalkCloudApp.ImMsgReqData.newBuilder().setId(userId).setSenderName(message.getFromUser().getDisplayName()).
                        setReceiverId(message.getReceiveId()).setReceiverType(2).setMsgType(1).setMsgCode(message.getMsgId()).setResourcePath(message.getText()).setSendTime(message.getTimeString()).build();
                reply = stub.imMessagePublish(data);
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
            try {
                channel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

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
                        .addFormDataPart("ReceiverType",2+"")
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
