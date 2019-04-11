package com.example.janusandroidtalk.activity;

import android.app.Dialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.janusandroidtalk.MainActivity;
import com.example.janusandroidtalk.MyApplication;
import com.example.janusandroidtalk.R;
import com.example.janusandroidtalk.bean.UserBean;
import com.example.janusandroidtalk.bean.UserFriendBean;
import com.example.janusandroidtalk.bean.UserGroupBean;
import com.example.janusandroidtalk.dialog.CustomProgressDialog;
import com.example.janusandroidtalk.floatwindow.FloatActionController;
import com.example.janusandroidtalk.tools.AppTools;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import talk_cloud.TalkCloudApp;
import talk_cloud.TalkCloudGrpc;

public class LoginActivity extends AppCompatActivity {

    private EditText editAccount;
    private EditText editPassword;
    private EditText editConfirm;

    private Button button;
    private TextView textGo;
    private TextView toolbarTitle;

    private boolean isRegister = false;

    private Dialog loading;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        editAccount = (EditText) findViewById(R.id.login_account);
        editPassword = (EditText) findViewById(R.id.login_password);
        editConfirm = (EditText) findViewById(R.id.login_confirm_password);

        button = (Button) findViewById(R.id.login_button);
        textGo = (TextView) findViewById(R.id.login_go_register);
        toolbarTitle = (TextView) findViewById(R.id.login_title);

//        editAccount.setText("xiaosong");
//        editPassword.setText("123456");
//        editConfirm.setText("123456");

        loading = CustomProgressDialog.createLoadingDialog(this,R.string.recycler_pull_loading);
        loading.setCancelable(true);
        loading.setCanceledOnTouchOutside(false);

        textGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isRegister){
                    isRegister = true;
                    button.setText(R.string.login_register);
                    textGo.setText(R.string.login_go_login);
                    toolbarTitle.setText(R.string.login_register);
                    editConfirm.setVisibility(View.VISIBLE);
                }else{
                    isRegister = false;
                    button.setText(R.string.login_login);
                    toolbarTitle.setText(R.string.login_login);
                    textGo.setText(R.string.login_go_register);
                    editConfirm.setVisibility(View.GONE);
                }
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isRegister){
                    //register
                    if(TextUtils.isEmpty(editAccount.getText().toString()) || TextUtils.isEmpty(editPassword.getText().toString()) ||TextUtils.isEmpty(editConfirm.getText().toString())){
                        Toast.makeText(getApplicationContext(), R.string.login_empty_tips, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if(!editPassword.getText().toString().equals(editConfirm.getText().toString())){
                        Toast.makeText(getApplicationContext(), R.string.login_inconsistent_tips, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    new GrpcRegisterTask().execute(editAccount.getText().toString(),editPassword.getText().toString());
                }else{
                    //login
                    if(TextUtils.isEmpty(editAccount.getText().toString()) || TextUtils.isEmpty(editPassword.getText().toString())){
                        Toast.makeText(getApplicationContext(), R.string.login_empty_tips, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    new GrpcLoginTask().execute(editAccount.getText().toString(),editPassword.getText().toString());
                }
            }
        });

    }

    class GrpcLoginTask extends AsyncTask<String, Void, TalkCloudApp.LoginRsp> {
        private ManagedChannel channel;

        private GrpcLoginTask() {
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loading.show();
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

            loading.dismiss();
            if(result == null){
                Toast.makeText(LoginActivity.this,R.string.request_data_null_tips,Toast.LENGTH_SHORT).show();
                return;
            }
            //判断result code
            if(result.getRes().getCode() != 200){
                Toast.makeText(LoginActivity.this,result.getRes().getMsg(),Toast.LENGTH_SHORT).show();
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
                userGroupBeanArrayList.add(userGroupBean);
            }
            userBean.setUserFriendBeanArrayList(userFriendBeanArrayList);
            userBean.setUserGroupBeanArrayList(userGroupBeanArrayList);
            UserBean.setUserBean(userBean);

            //判断是否存在默认进入的群组id，如果没有则默认第一个群组id
            if(MyApplication.getDefaultGroupId() == 0 || userGroupBeanArrayList.size()>0){
                MyApplication.setDefaultGroupId(userGroupBeanArrayList.get(0).getUserGroupId());
            }
            MyApplication.setUserId(result.getUserInfo().getId());
            MyApplication.setUserName(result.getUserInfo().getUserName());

            Toast.makeText(LoginActivity.this,R.string.login_login_success_tips,Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    class GrpcRegisterTask extends AsyncTask<String, Void, TalkCloudApp.AppRegRsp> {
        private ManagedChannel channel;

        private GrpcRegisterTask() {
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loading.show();
        }

        @Override
        protected TalkCloudApp.AppRegRsp doInBackground(String... params) {
            String name = params[0];
            String password = params[1];
            TalkCloudApp.AppRegRsp replay = null;
            try {
                channel = ManagedChannelBuilder.forAddress(AppTools.host, AppTools.port).usePlaintext().build();
                TalkCloudGrpc.TalkCloudBlockingStub stub = TalkCloudGrpc.newBlockingStub(channel);
                TalkCloudApp.AppRegReq regReq = TalkCloudApp.AppRegReq.newBuilder().setName(name).setPassword(password).build();
                replay = stub.appRegister(regReq);
                return replay;
            } catch (Exception e) {
               return replay;
            }
        }

        @Override
        protected void onPostExecute(TalkCloudApp.AppRegRsp result) {
            try {
                channel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            loading.dismiss();
            if(result == null){
                Toast.makeText(LoginActivity.this,R.string.request_data_null_tips,Toast.LENGTH_SHORT).show();
                return;
            }
            //判断result code
            if(result.getRes().getCode() != 200){
                Toast.makeText(LoginActivity.this,result.getRes().getMsg(),Toast.LENGTH_SHORT).show();
                return;
            }

            //保存数据
            UserBean userBean = new UserBean();
            userBean.setUserName(result.getUserName());
            userBean.setUserId(result.getId());
            UserBean.setUserBean(userBean);

            MyApplication.setUserId(result.getId());
            MyApplication.setUserName(result.getUserName());

            Toast.makeText(LoginActivity.this,R.string.login_register_success_tips,Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    // 物理返回键退出程序
    private long exitTime = 0;
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            if ((System.currentTimeMillis() - exitTime) > 3000) {
                Toast.makeText(getApplicationContext(), R.string.quit_tip, Toast.LENGTH_SHORT).show();
                exitTime =  System.currentTimeMillis();
            }else{
                finish();
            }
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

}
