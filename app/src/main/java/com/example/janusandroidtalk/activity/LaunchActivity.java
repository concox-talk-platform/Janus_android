package com.example.janusandroidtalk.activity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.example.janusandroidtalk.MainActivity;
import com.example.janusandroidtalk.MyApplication;
import com.example.janusandroidtalk.R;
import com.example.janusandroidtalk.bean.UserBean;
import com.example.janusandroidtalk.bean.UserFriendBean;
import com.example.janusandroidtalk.bean.UserGroupBean;
import com.example.janusandroidtalk.tools.AppTools;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import talk_cloud.TalkCloudApp;
import talk_cloud.TalkCloudGrpc;

public class LaunchActivity extends AppCompatActivity {

    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);

        Thread myThread=new Thread(){
            @Override
            public void run() {
                try{
                    sleep(1500);
                    Message message = new Message();
                    message.what = 1;
                    handler.sendMessage(message);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        };
        myThread.start();
    }

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.what == 1){
                Intent intent = new Intent(getApplicationContext(),LoginActivity.class);
                startActivity(intent);
                finish();
            }
        }
    };

    class GrpcLoginTask extends AsyncTask<String, Void, TalkCloudApp.LoginRsp> {
        private ManagedChannel channel;

        private GrpcLoginTask() {
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected TalkCloudApp.LoginRsp doInBackground(String... params) {
            String name = params[0];
            String password = params[1];
            TalkCloudApp.LoginRsp replay = null;
            try {
                channel = ManagedChannelBuilder.forAddress(AppTools.host, AppTools.port).usePlaintext().build();
                TalkCloudGrpc.TalkCloudBlockingStub stub = TalkCloudGrpc.newBlockingStub(channel);
                TalkCloudApp.LoginReq loginReq = TalkCloudApp.LoginReq.newBuilder().setName(name).setPasswd(password).build();
                replay = stub.login(loginReq);
                return replay;
            } catch (Exception e) {
                return replay;
            }
        }

        @Override
        protected void onPostExecute(TalkCloudApp.LoginRsp result) {
            try {
                channel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if(result == null){
                Toast.makeText(LaunchActivity.this,R.string.request_data_null_tips,Toast.LENGTH_SHORT).show();
                return;
            }
            //判断result code
            if(result.getRes().getCode() != 200){
                Toast.makeText(LaunchActivity.this,result.getRes().getMsg(),Toast.LENGTH_SHORT).show();
                return;
            }

            //保存数据
            ArrayList<UserFriendBean> userFriendBeanArrayList = new ArrayList<>();
            ArrayList<UserGroupBean> userGroupBeanArrayList = new ArrayList<>();
            UserBean userBean = new UserBean();
            userBean.setiMei(result.getUserInfo().getIMei());
            userBean.setNickName(result.getUserInfo().getNickName());
            userBean.setUserName(result.getUserInfo().getUserName());
            userBean.setUserId(result.getUserInfo().getId());
            for (TalkCloudApp.FriendRecord friendRecord: result.getFriendListList()) {
                UserFriendBean  userFriendBean = new UserFriendBean();
                userFriendBean.setUserFriendName(friendRecord.getName());
                userFriendBean.setUserFriendId(friendRecord.getUid());
                userFriendBeanArrayList.add(userFriendBean);
            }
            for (TalkCloudApp.GroupInfo groupRecord: result.getGroupListList()) {
                UserGroupBean userGroupBean = new UserGroupBean();
                userGroupBean.setUserGroupName(groupRecord.getGroupName());
                userGroupBean.setUserGroupId(groupRecord.getGid());
                ArrayList<UserFriendBean> memberList = new ArrayList<>();
                for (TalkCloudApp.UserRecord userRecord: groupRecord.getUsrListList()) {
                    UserFriendBean  userFriendBean1 = new UserFriendBean();
                    userFriendBean1.setUserFriendName(userRecord.getName());
                    userFriendBean1.setUserFriendId(userRecord.getUid());
                    userFriendBean1.setGroupRole(userRecord.getGrpRole());
                    userFriendBean1.setOnline(userRecord.getOnline());
                    memberList.add(userFriendBean1);
                }
                userGroupBean.setUserFriendBeanArrayList(memberList);
                userGroupBeanArrayList.add(userGroupBean);
            }
            userBean.setUserFriendBeanArrayList(userFriendBeanArrayList);
            userBean.setUserGroupBeanArrayList(userGroupBeanArrayList);
            UserBean.setUserBean(userBean);

            //判断是否存在默认进入的群组id，如果没有则默认第一个群组id
            if(MyApplication.getDefaultGroupId() == 0 && userGroupBeanArrayList.size()>0){
                MyApplication.setDefaultGroupId(userGroupBeanArrayList.get(0).getUserGroupId());
            }
            MyApplication.setUserId(result.getUserInfo().getId());
            MyApplication.setUserName(result.getUserInfo().getUserName());
            MyApplication.setLoginState("login");

            Toast.makeText(LaunchActivity.this,R.string.login_login_success_tips,Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(LaunchActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
