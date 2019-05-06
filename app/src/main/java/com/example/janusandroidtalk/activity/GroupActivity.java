package com.example.janusandroidtalk.activity;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.example.janusandroidtalk.R;
import com.example.janusandroidtalk.fragment.FragmentGroupInstantMessage;
import com.example.janusandroidtalk.fragment.FragmentGroupMember;
import com.example.janusandroidtalk.fragment.FragmentGroupTalkHistory;
import com.example.janusandroidtalk.signalingcontrol.MyControlCallBack;

import org.json.JSONObject;
import org.webrtc.MediaStream;

public class GroupActivity extends AppCompatActivity implements MyControlCallBack {
    //创建fragment变量
    private FragmentGroupMember fragmentGroupMember;
    private FragmentGroupInstantMessage fragmentGroupInstantMessage;
    private FragmentGroupTalkHistory fragmentGroupTalkHistory;

    //当前容器中的fragment
    private Fragment fragment_now = null;

    // Bottom navigation menu
    private BottomNavigationView.OnNavigationItemSelectedListener onGroupBottomNavigationMenuSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
            changePageFragment(menuItem.getItemId());
            return true;
        }
    };

    // 加载不同布局的Fragment页面
    // Loading different fragment page with different layout
    private void changePageFragment(int id) {
        switch (id) {
            case R.id.bottom_im:
                if (fragmentGroupInstantMessage == null){
                    fragmentGroupInstantMessage = fragmentGroupInstantMessage.newInstance();
                }
                switchFragment(fragment_now, fragmentGroupInstantMessage);
                break;
            case R.id.bottom_group_member:
                if (fragmentGroupMember == null){
                    fragmentGroupMember = fragmentGroupMember.newInstance();
                }
                switchFragment(fragment_now, fragmentGroupMember);
                break;
            case R.id.bottom_talk_history:
//                if (fragmentGroupTalkHistory == null){
//                    fragmentGroupTalkHistory = fragmentGroupTalkHistory.newInstance();
//                }
//                switchFragment(fragment_now, fragmentGroupTalkHistory);
                break;
        }
    }

    public void switchFragment(Fragment from, Fragment to) {
        if (to == null)
            return; // TODO Exception
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

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group);

        //底部导航栏
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.group_bottom_navigation);
        navigation.setOnNavigationItemSelectedListener(onGroupBottomNavigationMenuSelectedListener);
        //设置默认项目
        navigation.setSelectedItemId(R.id.bottom_group_member);
    }


    @Override
    public void janusServer(int code, String msg) {

    }

    @Override
    public void showMessage(JSONObject msg, JSONObject jsepLocal) {

    }

    @Override
    public void onSetLocalStream(MediaStream stream) {

    }

    @Override
    public void onAddRemoteStream(MediaStream stream) {

    }
}
