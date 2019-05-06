package com.example.janusandroidtalk.activity;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
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
import com.example.janusandroidtalk.grpcconnectionmanager.GrpcConnectionManager;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import talk_cloud.TalkCloudApp;

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

        GrpcConnectionManager.initGrpcConnectionManager();

        if(!TextUtils.isEmpty(MyApplication.getUserName()) && !TextUtils.isEmpty(MyApplication.getPassword()))
        editAccount.setText(MyApplication.getUserName());
        editPassword.setText(MyApplication.getPassword());

        loading = CustomProgressDialog.createLoadingDialog(this,R.string.recycler_pull_loading);
        loading.setCancelable(true);
        loading.setCanceledOnTouchOutside(false);

        textGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isRegister){
                    isRegister = true;
                    button.setText(R.string.login_register);
                    editAccount.setText("");
                    editPassword.setText("");
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

                    String account = editAccount.getText().toString();
                    String password = editPassword.getText().toString();
                    registerTask(account, password);
                }else{
                    //login
                    if(TextUtils.isEmpty(editAccount.getText().toString()) || TextUtils.isEmpty(editPassword.getText().toString())){
                        Toast.makeText(getApplicationContext(), R.string.login_empty_tips, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String account = editAccount.getText().toString();
                    String password = editPassword.getText().toString();
                    loginTask(account, password);
                }
            }
        });

        // Fix double-check-needed, but don't know why
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            finish();
            return;
        }
        applyPermission();
    }

    public void registerTask(String account, String password) {
        TalkCloudApp.AppRegReq appRegReq = TalkCloudApp.AppRegReq.newBuilder().setName(account).setPassword(password).build();
        TalkCloudApp.AppRegRsp appRegRsp = null;
        try {
            Future<TalkCloudApp.AppRegRsp> future = GrpcConnectionManager.getInstance().getGrpcInstantRequestHandler().submit(new Callable<TalkCloudApp.AppRegRsp>() {
                @Override
                public TalkCloudApp.AppRegRsp call() {
                    return GrpcConnectionManager.getInstance().getBlockingStub().appRegister(appRegReq);
                }
            });

            appRegRsp = future.get();
        } catch (Exception e) {

        }

        loading.dismiss();
        if(appRegRsp == null){
            Toast.makeText(LoginActivity.this,R.string.request_data_null_tips, Toast.LENGTH_SHORT).show();
            return;
        }
        //判断appRegRsp code
        if(appRegRsp.getRes().getCode() != 200){
            Toast.makeText(LoginActivity.this, appRegRsp.getRes().getMsg(), Toast.LENGTH_SHORT).show();
            return;
        }

        //保存数据
        UserBean userBean = new UserBean();
        userBean.setUserName(appRegRsp.getUserName());
        userBean.setUserId(appRegRsp.getId());
        UserBean.setUserBean(userBean);

        MyApplication.setUserId(appRegRsp.getId());
        MyApplication.setUserName(appRegRsp.getUserName());
        MyApplication.setPassword(editPassword.getText().toString());
        MyApplication.setLoginState("login");

        Toast.makeText(LoginActivity.this,R.string.login_register_success_tips,Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    public void loginTask(String account, String password) {
        TalkCloudApp.LoginReq loginReq = TalkCloudApp.LoginReq.newBuilder().setName(account).setPasswd(password).build();
        TalkCloudApp.LoginRsp loginRsp = null;
        try {
            Future<TalkCloudApp.LoginRsp> future = GrpcConnectionManager.getInstance().getGrpcInstantRequestHandler().submit(new Callable<TalkCloudApp.LoginRsp>() {
                @Override
                public TalkCloudApp.LoginRsp call() {
                    return GrpcConnectionManager.getInstance().getBlockingStub().login(loginReq);
                }
            });

            loginRsp = future.get();
        } catch (Exception e) {

        }

        loading.dismiss();
        if(loginRsp == null){
            Toast.makeText(LoginActivity.this, R.string.request_data_null_tips, Toast.LENGTH_SHORT).show();
            return;
        }
        //判断result code
        if(loginRsp.getRes().getCode() != 200){
            Toast.makeText(LoginActivity.this, loginRsp.getRes().getMsg(), Toast.LENGTH_SHORT).show();
            return;
        }

        //保存数据
        ArrayList<UserFriendBean> userFriendBeanArrayList = new ArrayList<>();
        ArrayList<UserGroupBean> userGroupBeanArrayList = new ArrayList<>();
        UserBean userBean = new UserBean();
        userBean.setiMei(loginRsp.getUserInfo().getIMei());
        userBean.setNickName(loginRsp.getUserInfo().getNickName());
        userBean.setUserName(loginRsp.getUserInfo().getUserName());
        userBean.setUserId(loginRsp.getUserInfo().getId());
        userBean.setUserLoginState(true);
        userBean.setOnline(loginRsp.getUserInfo().getOnline()); // 2 online, 1 offline

        //初始化好友列表
        for (TalkCloudApp.FriendRecord friendRecord: loginRsp.getFriendListList()) {
            UserFriendBean  userFriendBean = new UserFriendBean();
            userFriendBean.setUserFriendName(friendRecord.getName());
            userFriendBean.setUserFriendId(friendRecord.getUid());
            userFriendBeanArrayList.add(userFriendBean);
        }
        // 初始化群组列表
        for (TalkCloudApp.GroupInfo groupRecord: loginRsp.getGroupListList()) {
            UserGroupBean userGroupBean = new UserGroupBean();
            userGroupBean.setUserGroupName(groupRecord.getGroupName());
            userGroupBean.setUserGroupId(groupRecord.getGid());
            ArrayList<UserFriendBean> memberList = new ArrayList<>();

            // Inserting friends into group
            for (TalkCloudApp.UserRecord userRecord: groupRecord.getUsrListList()) {
                UserFriendBean  userFriendBean = new UserFriendBean();
                userFriendBean.setUserFriendName(userRecord.getName());
                userFriendBean.setUserFriendId(userRecord.getUid());
                userFriendBean.setOnline(userRecord.getOnline());
                if(groupRecord.getGroupManager() == userRecord.getUid()){
                    userGroupBean.setGroupManagerId(userFriendBean.getUserFriendId());
                }

                memberList.add(userFriendBean);
            }
            userGroupBean.setUserFriendBeanArrayList(memberList);
            userGroupBeanArrayList.add(userGroupBean);
        }
        userBean.setUserFriendBeanArrayList(userFriendBeanArrayList);
        userBean.setUserGroupBeanArrayList(userGroupBeanArrayList);
        UserBean.setUserBean(userBean);

        MyApplication.setDefaultGroupId(loginRsp.getUserInfo().getLockGroupId());

        //判断是否存在默认进入的群组id，如果没有则默认第一个群组id
        if(MyApplication.getDefaultGroupId() == 0 && userGroupBeanArrayList.size() > 0){
            MyApplication.setDefaultGroupId(userGroupBeanArrayList.get(0).getUserGroupId());
        }
        MyApplication.setUserId(loginRsp.getUserInfo().getId());
        MyApplication.setUserName(loginRsp.getUserInfo().getUserName());
        MyApplication.setPassword(editPassword.getText().toString());
        MyApplication.setLoginState("login");

        Toast.makeText(LoginActivity.this, R.string.login_login_success_tips, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
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
                // 关闭Grpc连接
                // Shutting down GrpcConnectionManager
                GrpcConnectionManager.closeGrpcConnectionManager();

                finish();
            }
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 申请权限
     */
    private void applyPermission(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            }else {
                requestPermissions(new String[]{Manifest.permission
                        .ACCESS_FINE_LOCATION}, 2);
            }

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            }else {
                requestPermissions(new String[]{Manifest.permission
                        .READ_PHONE_STATE}, 3);
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            }else {
                requestPermissions(new String[]{Manifest.permission
                        .ACCESS_COARSE_LOCATION}, 4);
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            }else {
                requestPermissions(new String[]{Manifest.permission
                        .RECORD_AUDIO}, 5);
            }
        }
    }
}
