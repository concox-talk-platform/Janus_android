package com.example.janusandroidtalk.activity;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
                    handleRegisterTaskBack(account, password);
                }else{
                    //login
                    if(TextUtils.isEmpty(editAccount.getText().toString()) || TextUtils.isEmpty(editPassword.getText().toString())){
                        Toast.makeText(getApplicationContext(), R.string.login_empty_tips, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String account = editAccount.getText().toString();
                    String password = editPassword.getText().toString();
                    handleLoginTaskBack(account, password);
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

    public void handleRegisterTaskBack(String account, String password) {
        loading.show();
        TalkCloudApp.AppRegReq appRegReq = TalkCloudApp.AppRegReq.newBuilder().setName(account).setPassword(password).build();

        try {
            GrpcConnectionManager.getInstance().getGrpcInstantRequestHandler().submit(new Runnable() {
                @Override
                public void run() {
                    TalkCloudApp.AppRegRsp appRegRsp = GrpcConnectionManager.getInstance().getBlockingStub().appRegister(appRegReq);

                    Message msg = Message.obtain();
                    msg.obj = appRegRsp;
                    msg.what = 2;
                    handler.sendMessage(msg);
                }
            });
        } catch (Exception e) {

        }
    }

    public void registerTask(TalkCloudApp.AppRegRsp appRegRsp) {
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

        loading.dismiss();

        Toast.makeText(LoginActivity.this,R.string.login_register_success_tips,Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    public void handleLoginTaskBack(String account, String password) {
        loading.show();
        TalkCloudApp.LoginReq loginReq = TalkCloudApp.LoginReq.newBuilder().setName(account).setPasswd(password).build();

        try {
            GrpcConnectionManager.getInstance().getGrpcInstantRequestHandler().submit(new Runnable() {
                @Override
                public void run() {
                    TalkCloudApp.LoginRsp loginRsp = GrpcConnectionManager.getInstance().getBlockingStub().login(loginReq);

                    Message msg = Message.obtain();
                    msg.obj = loginRsp;
                    msg.what = 1;   //标志消息的标志
                    handler.sendMessage(msg);
                }
            });
        } catch (Exception e) {
            //TODO Nothing here
        }
    }

    public void loginTask(TalkCloudApp.LoginRsp loginRsp) {
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

        UserBean.getUserBean().setiMei(loginRsp.getUserInfo().getIMei());
        UserBean.getUserBean().setNickName(loginRsp.getUserInfo().getNickName());
        UserBean.getUserBean().setUserName(loginRsp.getUserInfo().getUserName());
        UserBean.getUserBean().setUserId(loginRsp.getUserInfo().getId());
        UserBean.getUserBean().setUserLoginState(true);
        UserBean.getUserBean().setOnline(loginRsp.getUserInfo().getOnline());   // 2 online, 1 offline

        //初始化好友列表
        for (TalkCloudApp.FriendRecord friendRecord: loginRsp.getFriendListList()) {
            UserFriendBean userFriendBean = new UserFriendBean();
            userFriendBean.setUserFriendBeanObjByFriendRecord(friendRecord);
            userFriendBeanArrayList.add(userFriendBean);
        }
        UserBean.getUserBean().setUserFriendBeanArrayList(userFriendBeanArrayList);

        // 初始化群组列表
        for (TalkCloudApp.GroupInfo groupInfo: loginRsp.getGroupListList()) {
            UserGroupBean userGroupBean = new UserGroupBean();
            userGroupBean.setUserGroupBeanObj(groupInfo);

            userGroupBeanArrayList.add(userGroupBean);
        }
        UserBean.getUserBean().setUserGroupBeanArrayList(userGroupBeanArrayList);

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
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission
                        .ACCESS_FINE_LOCATION}, 102);
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission
                        .READ_PHONE_STATE}, 103);
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission
                        .ACCESS_COARSE_LOCATION}, 104);
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission
                        .RECORD_AUDIO}, 105);
            }
        }
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {      //判断标志位
                case 1:
                    loginTask((TalkCloudApp.LoginRsp)msg.obj);
                    break;
                case 2:
                    registerTask((TalkCloudApp.AppRegRsp)msg.obj);
                    break;
            }
        }
    };
}
