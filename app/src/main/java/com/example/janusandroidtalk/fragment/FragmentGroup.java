package com.example.janusandroidtalk.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.janusandroidtalk.MyApplication;
import com.example.janusandroidtalk.R;
import com.example.janusandroidtalk.activity.GroupActivity;
import com.example.janusandroidtalk.activity.GroupCreateActivity;
import com.example.janusandroidtalk.activity.SearchActivity;
import com.example.janusandroidtalk.bean.UserBean;
import com.example.janusandroidtalk.bean.UserFriendBean;
import com.example.janusandroidtalk.bean.UserGroupBean;
import com.example.janusandroidtalk.floatwindow.FloatActionController;
import com.example.janusandroidtalk.floatwindow.permission.FloatPermissionManager;
import com.example.janusandroidtalk.grpcconnectionmanager.GrpcConnectionManager;
import com.example.janusandroidtalk.im.activity.GroupChatActivity;
import com.example.janusandroidtalk.pullrecyclerview.BaseRecyclerAdapter;
import com.example.janusandroidtalk.pullrecyclerview.BaseViewHolder;
import com.example.janusandroidtalk.pullrecyclerview.PullRecyclerView;
import com.example.janusandroidtalk.pullrecyclerview.layoutmanager.XLinearLayoutManager;
import com.example.janusandroidtalk.signalingcontrol.JanusControl;
import com.example.janusandroidtalk.signalingcontrol.MyControlCallBack;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.MediaStream;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import talk_cloud.TalkCloudApp;

public class FragmentGroup extends Fragment implements MyControlCallBack{

    private PullRecyclerView mPullRecyclerView;
    private List<UserGroupBean> myList = new ArrayList<>();
    private GroupListAdapter mAdapter;

    private LinearLayout searchView;

    private ImageView menu;

    private JanusControl janusControl;

    private int changeroomid;

    public FragmentGroup() {
    }

    public static FragmentGroup newInstance() {
        FragmentGroup workfragment = new FragmentGroup();
        return workfragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_group, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mPullRecyclerView = getActivity().findViewById(R.id.group_list);
        searchView = getActivity().findViewById(R.id.group_layout);
        menu = (ImageView)getActivity().findViewById(R.id.group_menu);

        mPullRecyclerView.setLayoutManager(new XLinearLayoutManager(getActivity()));
        mPullRecyclerView.setColorSchemeResources(R.color.colorMain);
        mPullRecyclerView.enableLoadMore(false);
        //mPullRecyclerView.enableLoadDoneTip(true,R.string.recycler_pull_done);

        if (UserBean.getUserBean() != null && UserBean.getUserBean().getUserGroupBeanArrayList() != null) {
            //mPullRecyclerView.postRefreshing();
            //new GrpcGetGroupListTask().execute(UserBean.getUserBean().getUserId()+"");
            myList = UserBean.getUserBean().getUserGroupBeanArrayList();
        }

        mAdapter = new GroupListAdapter(getActivity(), R.layout.fragment_group_list_item, myList);
        mPullRecyclerView.setAdapter(mAdapter);

        janusControl = new JanusControl(this,MyApplication.getUserName(),MyApplication.getUserId(),MyApplication.getDefaultGroupId());
        janusControl.Start();

        //第一次进来发起请求
        if (UserBean.getUserBean() != null) {
            mPullRecyclerView.postRefreshing();
            updateGroupsInfo();
            mPullRecyclerView.stopRefresh();
        }
        mPullRecyclerView.setOnRecyclerRefreshListener(new PullRecyclerView.OnRecyclerRefreshListener() {
            @Override
            public void onPullRefresh() {
                // 下拉刷新事件被触发
                if (UserBean.getUserBean() != null) {
                    updateGroupsInfo();
                    mPullRecyclerView.stopRefresh();
                }
            }

            @Override
            public void onLoadMore() {
                // 上拉加载更多事件被触发
            }
        });

        //跳转到搜索页面
        searchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), SearchActivity.class);
                intent.putExtra("searchType", 0);
                startActivity(intent);
            }
        });

        //创建群组
        menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), GroupCreateActivity.class));
            }
        });
    }

    // Updating groups' info. Attentions someone invited you into a group, so we need pull user's groups info from server and update local
    public void updateGroupsInfo() {
        TalkCloudApp.GrpListReq grpListReq = TalkCloudApp.GrpListReq.newBuilder().setUid(UserBean.getUserBean().getUserId()).build();
        TalkCloudApp.GroupListRsp groupListRsp = null;
        try {
            Future<TalkCloudApp.GroupListRsp> future = GrpcConnectionManager.getInstance().getGrpcInstantRequestHandler().submit(new Callable<TalkCloudApp.GroupListRsp>() {
                @Override
                public TalkCloudApp.GroupListRsp call() {
                    return GrpcConnectionManager.getInstance().getBlockingStub().getGroupList(grpListReq);
                }
            });

            groupListRsp = future.get();
        } catch (Exception e) {
            //TODO Exception catch later
        }

        if (groupListRsp == null) {
            Toast.makeText(getContext(), R.string.request_data_null_tips, Toast.LENGTH_SHORT).show();
            return;
        }
        //判断result code
        if(groupListRsp.getRes().getCode() != 200){
            Toast.makeText(getContext(), groupListRsp.getRes().getMsg(), Toast.LENGTH_SHORT).show();
            return;
        }

        if(myList.size() != 0){
            myList.clear();
        }

        // 更新用户群组信息
        // Updating User's info
        ArrayList<UserGroupBean> userGroupBeanArrayList = new ArrayList<UserGroupBean>();
        for (TalkCloudApp.GroupInfo groupRecord : groupListRsp.getGroupListList()) {
            UserGroupBean userGroupBean = new UserGroupBean();
            userGroupBean.setUserGroupId(groupRecord.getGid());
            userGroupBean.setUserGroupName(groupRecord.getGroupName());
            ArrayList<UserFriendBean> memberList = new ArrayList<>();

            // Inserting friends into group
            for (TalkCloudApp.UserRecord userRecord: groupRecord.getUsrListList()) {
                UserFriendBean userFriendBean = new UserFriendBean();
                userFriendBean.setUserFriendName(userRecord.getName());
                userFriendBean.setUserFriendId(userRecord.getUid());
                userFriendBean.setGroupRole(userRecord.getGrpRole());
                userFriendBean.setOnline(userRecord.getOnline());
                if(groupRecord.getGroupManager() == userRecord.getUid()){
                    userGroupBean.setGroupManagerId(userFriendBean.getUserFriendId());
                }
                memberList.add(userFriendBean);
            }
            userGroupBean.setUserFriendBeanArrayList(memberList);
            userGroupBeanArrayList.add(userGroupBean);
        }
        myList.addAll(userGroupBeanArrayList);
        mAdapter.notifyDataSetChanged();

        //保存group信息
        if (UserBean.getUserBean() != null) {
            UserBean.getUserBean().setUserGroupBeanArrayList(userGroupBeanArrayList);
        }

        //如果没有默认房间，创建新群组刷新之后，将会默认进入第一个群组里面，
        if(MyApplication.getDefaultGroupId() == 0 && userGroupBeanArrayList.size()>0){
            MyApplication.setDefaultGroupId(userGroupBeanArrayList.get(0).getUserGroupId());
            JanusControl.sendPocRoomJoinRoom(FragmentGroup.this,MyApplication.getDefaultGroupId());
        }
    }

    class GroupListAdapter extends BaseRecyclerAdapter{

        private Context context;

        public GroupListAdapter(Context context, int layoutResId, List<UserGroupBean> data) {
            super(context, layoutResId, data);
            this.context = context;
        }

        @Override
        protected void converted(BaseViewHolder holder, Object item, int position) {
//            final UserGroupBean data = myList.get(position);
            final UserGroupBean data = UserBean.getUserBean().getUserGroupBeanArrayList().get(position);
//            String onlineState = "(" + data.getOnlineMembersCount() + "/" + data.getTotalMembersCount() + ")";
            String onlineState = "(" + data.getOnlineMembersCountLocal() + "/" + data.getTotalMembersCount() + ")";
            holder.setText(R.id.fragment_group_list_item_name, data.getUserGroupName() + onlineState);
            holder.setText(R.id.fragment_group_list_item_state, data.getUserGroupId() + "");
            ImageView imageView_im = holder.getView(R.id.fragment_group_list_item_im);
            ImageView imageView_lock = holder.getView(R.id.fragment_group_list_item_lock);
            LinearLayout linearLayout = holder.getView(R.id.fragment_group_list_item);

            imageView_im.setImageResource(R.drawable.ic_chat_black_24dp);
            // TODO Set imageView_im clickListener
            imageView_im.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getActivity(), GroupChatActivity.class);
                    intent.putExtra("usergroupid",data.getUserGroupId());
                    intent.putExtra("usergroupname",data.getUserGroupName());
                    startActivity(intent);
                }
            });

            if (data.getUserGroupId() == MyApplication.getDefaultGroupId()) {
                imageView_lock.setImageResource(R.drawable.ic_lock_outline_black_24dp);
            } else {
                imageView_lock.setImageResource(R.drawable.ic_lock_open_black_24dp);
            }

            linearLayout.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getContext(), GroupActivity.class);
                    intent.putExtra("groupPosition", position);
                    startActivity(intent);
                }
            });

            imageView_lock.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final AlertDialog dialog = new AlertDialog.Builder(context)
                            .setMessage(R.string.dialog_message_change_group)
                            .setPositiveButton(R.string.dialog_commit, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //发送changeRoom信令 ,data.get("gid"),并在回调成功之后，保存默认群组Id,
                                    JanusControl.sendChangeGroup(FragmentGroup.this, data.getUserGroupId());
                                    changeroomid = data.getUserGroupId();

                                    MyApplication.setDefaultGroupId(changeroomid);
                                    setLockGroupId(changeroomid);//TODO
                                    dialog.dismiss();
                                }
                            })
                            .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            }).create();
                    dialog.show();
                }
            });
        }
    }

    @Override
    public void janusServer(int code,String msg) {
       switch (code){
           case 101:
               JanusControl.sendAttachPocRoomPlugin(this,false);
           break;

       }
    }

    @Override
    public void showMessage(JSONObject msg,JSONObject jsepLocal) {
        try {
            if (msg.getString("pocroom").equals("audiobridgeisok")) {
                //创建链接成功，
                JanusControl.janusControlCreatePeerConnectionFactory(getActivity());
                if(MyApplication.getDefaultGroupId()!=0){//不为空才直接加入房间
                    JanusControl.sendPocRoomJoinRoom(FragmentGroup.this,MyApplication.getDefaultGroupId());
                }
            }else if(msg.getString("pocroom").equals("joined")){//加入房间成功，开始创建offer,进行webRtc链接
                if(msg.has("id") && msg.getInt("id") == MyApplication.getUserId() ){
                    JanusControl.sendPocRoomCreateOffer(FragmentGroup.this);
                }
            }else if(msg.getString("pocroom").equals("event")){//"configure" 信令成功

            }else if(msg.getString("pocroom").equals("webRtcisok")){//webRtc链接成功
                Message message2 = new Message();
                message2.what = 2;
                handler.sendMessage(message2);
            }else if (msg.getString("pocroom").equals("roomchanged")) {
                //保存新的默认群组ID
                //MyApplication.setDefaultGroupId(msg.getInt("room"));
                Message message = new Message();
                message.what = 3;
                handler.sendMessage(message);
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

    private Handler handler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case 1:
                    mAdapter.notifyDataSetChanged();
                    break;
                case 2:
                    //webRtc链接成功之后，开启悬浮窗
                    boolean isPermission = FloatPermissionManager.getInstance().applyFloatWindow(getActivity());
                    //有对应权限或者系统版本小于7.0
                    if (isPermission || Build.VERSION.SDK_INT < 24) {
                        //开启悬浮窗
                        FloatActionController.getInstance().startMonkServer(getActivity());
                    }
                    break;
                case 3:
                    MyApplication.setDefaultGroupId(changeroomid);
                    setLockGroupId(changeroomid);//TODO
                    mAdapter.notifyDataSetChanged();
                    break;
            }
        };
    };

    public void setLockGroupId(int lockGroupId) {
        int gid = lockGroupId;
        int uid = UserBean.getUserBean().getUserId();
        TalkCloudApp.SetLockGroupIdReq setLockGroupIdReq = TalkCloudApp.SetLockGroupIdReq.newBuilder().setGId(gid).setUId(uid).build();
        TalkCloudApp.SetLockGroupIdResp setLockGroupIdResp = null;
        try {
            Future<TalkCloudApp.SetLockGroupIdResp> future = GrpcConnectionManager.getInstance().getGrpcInstantRequestHandler().submit(new Callable<TalkCloudApp.SetLockGroupIdResp>() {
                @Override
                public TalkCloudApp.SetLockGroupIdResp call() throws Exception {
                    return GrpcConnectionManager.getInstance().getBlockingStub().setLockGroupId(setLockGroupIdReq);
                }
            });

            setLockGroupIdResp = future.get(); // TODO No use?
        } catch (Exception e) {
            // TODO Nothing here
        }
    }
}
