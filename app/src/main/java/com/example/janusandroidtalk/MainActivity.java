package com.example.janusandroidtalk;

import android.opengl.EGLContext;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.janusandroidtalk.floatwindow.FloatActionController;
import com.example.janusandroidtalk.floatwindow.permission.FloatPermissionManager;
import com.example.janusandroidtalk.fragment.FragmentGroup;
import com.example.janusandroidtalk.fragment.FragmentMine;
import com.example.janusandroidtalk.signalingcontrol.AudioBridgeControl;
import com.example.janusandroidtalk.webrtctest.AppRTCAudioManager;

import org.webrtc.VideoRendererGui;

public class MainActivity extends AppCompatActivity {

    //创建fragment变量
    private FragmentMine fragmentMine;
    private FragmentGroup fragmentGroup;
    //当前容器中的fragment
    private Fragment fragment_now = null;

    //audioManager
    private AppRTCAudioManager audioManager = null;
    private AudioBridgeControl audioBridgeControl;

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
//            //启动加入房间
//            EGLContext con = VideoRendererGui.getEGLContext();
//            audioBridgeControl = new AudioBridgeControl(MyApplication.getUserName(),MyApplication.getUserId(),MyApplication.getDefaultGroupId());
//            audioBridgeControl.initializeMediaContext(MainActivity.this, true, false, false, con);
//            audioBridgeControl.Start();
//        }

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
                break;
            case R.id.navigation_group:
                if (fragmentGroup == null){
                    fragmentGroup = fragmentGroup.newInstance();
                }
                switchFragment(fragment_now, fragmentGroup);
                break;
        }
    }

    public void switchFragment(Fragment from, Fragment to) {
        if (to == null)
            return;
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (!to.isAdded()) {
            if (from == null) {
                transaction.add(R.id.frame_layout, to).show(to).commit();
            } else {
                transaction.hide(from).add(R.id.frame_layout, to).commitAllowingStateLoss();
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
                MyApplication.clearMyData();
                FloatActionController.getInstance().stopMonkServer(this);
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
    protected void onRestart() {
        super.onRestart();
        if(FloatActionController.getInstance() != null && !FloatActionController.getInstance().isShow()){
            FloatActionController.getInstance().show();
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
}
