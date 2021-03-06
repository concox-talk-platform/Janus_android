package com.example.janusandroidtalk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.janusandroidtalk.floatwindow.FloatActionController;
import com.example.janusandroidtalk.fragment.FragmentGroup;
import com.example.janusandroidtalk.fragment.FragmentMine;
import com.example.janusandroidtalk.gps.LocationService;
import com.example.janusandroidtalk.grpcconnectionmanager.GrpcConnectionManager;
import com.example.janusandroidtalk.grpcconnectionmanager.ToFragmentListener;
import com.example.janusandroidtalk.signalingcontrol.JanusControl;
import com.example.janusandroidtalk.signalingcontrol.MyControlCallBack;
import com.example.janusandroidtalk.tools.AppTools;
import com.example.janusandroidtalk.webrtc.AppRTCAudioManager;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.MediaStream;

public class MainActivity extends AppCompatActivity implements MyControlCallBack {

    //创建fragment变量
    private FragmentMine fragmentMine;
    private FragmentGroup fragmentGroup;
    //当前容器中的fragment
    private Fragment fragment_now = null;

    private ToFragmentListener toFragmentListener;
    private RealTimeUpdaterTest realTimeUpdaterTest;
    private IntentFilter intentFilter;

    //audioManager
    private AppRTCAudioManager audioManager = null;
    private JanusControl janusControl = null;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //底部导航栏
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        //设置默认项目
        navigation.setSelectedItemId(R.id.navigation_group);

        audioManager = AppRTCAudioManager.create(MainActivity.this, new Runnable() {
                    // This method will be called each time the audio state (number and
                    // type of devices) has been changed.
                    @Override
                    public void run() {
                        onAudioManagerChangedState();
                    }
                }
        );
        audioManager.init();

//        if(MyApplication.getDefaultGroupId() != 0){
//            boolean isPermission = FloatPermissionManager.getInstance().applyFloatWindow(this);
//            //有对应权限或者系统版本小于7.0
//            if (isPermission || Build.VERSION.SDK_INT < 24) {
//                //开启悬浮窗
//                FloatActionController.getInstance().startMonkServer(this);
//            }
            //启动加入房间
//            janusControl = new JanusControl(MainActivity.this,MyApplication.getUserName(),MyApplication.getUserId(),MyApplication.getDefaultGroupId());
//            janusControl.Start();
//        }

        // 进行webSocket连接
//        janusControl = new JanusControl(MainActivity.this,MyApplication.getUserName(),MyApplication.getUserId(),MyApplication.getDefaultGroupId());
//        janusControl.Start();

        //启动LocationService
        if(!AppTools.isServiceRunning(this,"LocationService")){
            Intent intent = new Intent(this, LocationService.class);
            startService(intent);
        }

        //实时渲染
        realTimeUpdaterTest = new RealTimeUpdaterTest();
        intentFilter = new IntentFilter();
        intentFilter.addAction("123456");

        registerReceiver(realTimeUpdaterTest, intentFilter);

        handler.removeMessages(100);
        handler.sendEmptyMessage(100);
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            changePageFragment(item.getItemId());
            return true;
        }
    };

    public void changePageFragment(int id) {
        switch (id) {
            case R.id.navigation_mine:
                if (fragmentMine == null){
                    fragmentMine = fragmentMine.newInstance();
                }
                switchFragment(fragment_now, fragmentMine);
                toFragmentListener = fragmentMine;
                break;
            case R.id.navigation_group:
                if (fragmentGroup == null){
                    fragmentGroup = fragmentGroup.newInstance();
                }
                switchFragment(fragment_now, fragmentGroup);
                toFragmentListener = fragmentGroup;
                break;
        }
    }

    public void switchFragment(Fragment from, Fragment to) {
        if (to == null)
            return;
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (!to.isAdded()) {
            if (from == null) {
                transaction.add(R.id.activity_main_frame_layout, to).show(to).commit();
            } else {
                transaction.hide(from).add(R.id.activity_main_frame_layout, to).commitAllowingStateLoss();
            }
        } else {
            transaction.hide(from).show(to).commit();
        }
        fragment_now = to;
    }

    // 物理返回键退出程序
    private long exitTime = 0;
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            if ((System.currentTimeMillis() - exitTime) > 3000) {
                Toast.makeText(getApplicationContext(),  R.string.quit_tip, Toast.LENGTH_SHORT).show();
                exitTime =  System.currentTimeMillis();
            }else{
                JanusControl.closeWebRtc();
                JanusControl.closeJanusServer();
                GrpcConnectionManager.closeGrpcConnectionManager();
                FloatActionController.getInstance().stopMonkServer(this);

                unregisterReceiver(realTimeUpdaterTest);

                finish();
            }
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void onAudioManagerChangedState() {
        // TODO(henrika): disable video if AppRTCAudioManager.AudioDevice.EARPIECE
        // is active.
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(FloatActionController.getInstance() != null && !FloatActionController.getInstance().isShow()){
            FloatActionController.getInstance().show();
        }

        registerReceiver(realTimeUpdaterTest, intentFilter);
    }

    private class RealTimeUpdaterTest extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String intentAction = intent.getAction();
            if (intentAction.equals("123456")) {
                Log.d("MainActivity"," this is test RealTimeUpdaterTest onReceive get message from broadcast");
                toFragmentListener.dynamicTransfer("Refresh Page Now!!!");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //关闭悬浮窗
        FloatActionController.getInstance().stopMonkServer(this);
        if (audioManager != null) {
            audioManager.close();
            audioManager = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(realTimeUpdaterTest);
    }

    @Override
    public void janusServer(int code,String msg) {
        switch (code){
            case 0:
//                Message message = new Message();
//                message.what = 0;
//                handler.sendMessage(message);
                break;
            case 100:
//                Message message1 = new Message();
//                message1.what = 1;
//                handler.sendMessage(message1);
                break;
        }
    }

    @Override
    public void showMessage(JSONObject msg ,JSONObject jsepLocal) {
        try {
            if (msg.getString("videocall").equals("videoCallIsOk")) {
                JanusControl.sendRegister();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSetLocalStream(MediaStream stream) {

    }

    @Override
    public void onAddRemoteStream(MediaStream stream) {

    }

    private void sendReceiverBroadcast() {
        Intent intent = new Intent();
        intent.setAction("123456");
        sendBroadcast(intent);
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    //Toast.makeText(MainActivity.this,"webSocket连接成功",Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    //Toast.makeText(MainActivity.this,"webSocket连接失败",Toast.LENGTH_SHORT).show();
                    break;
                case 100: // Refresh
                    sendReceiverBroadcast();
                    handler.sendEmptyMessageDelayed(100, 2*1000);
                    break;
            }
        };
    };

}
