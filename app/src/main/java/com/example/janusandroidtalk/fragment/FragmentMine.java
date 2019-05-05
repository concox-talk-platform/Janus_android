package com.example.janusandroidtalk.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.TextView;
import android.widget.Toast;


import com.example.janusandroidtalk.MyApplication;
import com.example.janusandroidtalk.R;

import com.example.janusandroidtalk.activity.CallActivity;
import com.example.janusandroidtalk.activity.LoginActivity;
import com.example.janusandroidtalk.activity.SearchActivity;
import com.example.janusandroidtalk.bean.UserBean;
import com.example.janusandroidtalk.bean.UserFriendBean;
import com.example.janusandroidtalk.floatwindow.FloatActionController;
import com.example.janusandroidtalk.grpcconnectionmanager.GrpcConnectionManager;
import com.example.janusandroidtalk.pullrecyclerview.BaseRecyclerAdapter;
import com.example.janusandroidtalk.pullrecyclerview.BaseViewHolder;
import com.example.janusandroidtalk.pullrecyclerview.PullRecyclerView;
import com.example.janusandroidtalk.pullrecyclerview.layoutmanager.XLinearLayoutManager;
import com.example.janusandroidtalk.signalingcontrol.JanusControl;
import com.example.janusandroidtalk.signalingcontrol.MyControlCallBack;

import org.json.JSONObject;
import org.webrtc.MediaStream;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import talk_cloud.TalkCloudApp;

public class FragmentMine extends Fragment implements MyControlCallBack {

    private PullRecyclerView mPullRecyclerView;
    private List<UserFriendBean> myList = new ArrayList<>();
    private MineListAdapter mAdapter;

    private LinearLayout searchView;
    private TextView imageView;

    private String name;
    private int remoteId;
    private boolean isVideo;


    public FragmentMine() {

    }

    public static FragmentMine newInstance() {
        FragmentMine mineFragment = new FragmentMine();
        return mineFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mine, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mPullRecyclerView = getActivity().findViewById(R.id.mine_list);
        searchView  = getActivity().findViewById(R.id.mine_layout);
        imageView = getActivity().findViewById(R.id.mine_menu);

        mPullRecyclerView.setLayoutManager(new XLinearLayoutManager(getActivity()));
        mAdapter = new MineListAdapter(getActivity(), R.layout.fragment_mine_list_item, myList);
        mPullRecyclerView.setAdapter(mAdapter);
        mPullRecyclerView.setColorSchemeResources(R.color.colorMain);
        mPullRecyclerView.enableLoadMore(false);
        //mPullRecyclerView.enableLoadDoneTip(true,R.string.recycler_pull_done);

        //第一次进来发起请求
        if (UserBean.getUserBean() != null) {
            mPullRecyclerView.postRefreshing();
            updateFriendsInfo();
        }
        //下拉刷新
        mPullRecyclerView.setOnRecyclerRefreshListener(new PullRecyclerView.OnRecyclerRefreshListener() {
            @Override
            public void onPullRefresh() {
                // 下拉刷新事件被触发
                if (UserBean.getUserBean() != null) {
                    updateFriendsInfo();
                }
            }

            @Override
            public void onLoadMore() {
                // 上拉加载更多事件被触发
            }
        });

        //跳转到搜索页面
        searchView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), SearchActivity.class);
                intent.putExtra("searchType",1);
                startActivity(intent);
            }
        });

        //推出按钮
        imageView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                        .setMessage(R.string.dialog_message_exit)
                        .setPositiveButton(R.string.dialog_commit, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                MyApplication.clearMyData();
                                FloatActionController.getInstance().stopMonkServer(getActivity());
                                JanusControl.closeWebRtc();
                                JanusControl.closeJanusServer();
                                GrpcConnectionManager.closeGrpcConnectionManager();
                                Intent intent = new Intent(getActivity(),LoginActivity.class);
                                getActivity().startActivity(intent);
                                getActivity().finish();
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

    //Updating friends' info
    public void updateFriendsInfo() {
        TalkCloudApp.FriendsReq friendsReq = TalkCloudApp.FriendsReq.newBuilder().setUid(UserBean.getUserBean().getUserId()).build();
        TalkCloudApp.FriendsRsp friendsRsp = null;
        try {
            Future<TalkCloudApp.FriendsRsp> future = GrpcConnectionManager.getInstance().getGrpcInstantRequestHandler().submit(new Callable<TalkCloudApp.FriendsRsp>() {
                @Override
                public TalkCloudApp.FriendsRsp call() {
                    return GrpcConnectionManager.getInstance().getBlockingStub().getFriendList(friendsReq);
                }
            });

            friendsRsp = future.get();
        } catch (Exception e) {
            //TODO nothing here
        }

        mPullRecyclerView.stopRefresh();

        if(friendsRsp == null){
            Toast.makeText(getContext(), R.string.request_data_null_tips, Toast.LENGTH_SHORT).show();
            return;
        }
        //判断friendsRsp code
        if(friendsRsp.getRes().getCode() != 200){
            Toast.makeText(getContext(), friendsRsp.getRes().getMsg(), Toast.LENGTH_SHORT).show();
            return;
        }

        myList.clear();
        ArrayList<UserFriendBean> userFriendBeanArrayList = new ArrayList<UserFriendBean>();
        for (TalkCloudApp.FriendRecord friendRecord : friendsRsp.getFriendListList()) {
            UserFriendBean userFriendBean = new UserFriendBean();
            userFriendBean.setUserFriendId((int)friendRecord.getUid());
            userFriendBean.setUserFriendName(friendRecord.getName());
            userFriendBeanArrayList.add(userFriendBean);
        }
        myList.addAll(userFriendBeanArrayList);
        mAdapter.notifyDataSetChanged();
        if (UserBean.getUserBean() != null) {
            UserBean.getUserBean().setUserFriendBeanArrayList(userFriendBeanArrayList);
        }
    }

    class MineListAdapter extends BaseRecyclerAdapter {
        private Context context;
        public MineListAdapter(Context context, int layoutResId, List<UserFriendBean> data) {
            super(context, layoutResId, data);
            this.context = context;
        }
        @Override
        protected void converted(BaseViewHolder holder, Object item, int position) {
            final UserFriendBean data = myList.get(position);
            holder.setText(R.id.fragment_mine_list_item_name,data.getUserFriendName());
            holder.setText(R.id.fragment_mine_list_item_state,data.getUserFriendId()+"");
            ImageView imageViewVideo = holder.getView(R.id.fragment_mine_list_item_video_call);
            ImageView imageViewAudio = holder.getView(R.id.fragment_mine_list_item_audio_call);
            imageViewVideo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                            .setMessage(R.string.dialog_message_quit_group_room_for_video)
                            .setPositiveButton(R.string.dialog_commit, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    //发送关闭pocRoom插件，关闭webrtc连接
                                    //JanusControl.janusControlDetach(FragmentMine.this,false);
                                    JanusControl.janusControlPocRoomDetach(FragmentMine.this);
                                    name = data.getUserFriendName();
                                    remoteId = data.getUserFriendId();
                                    isVideo = true;
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

            imageViewAudio.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                            .setMessage(R.string.dialog_message_quit_group_room_for_audio)
                            .setPositiveButton(R.string.dialog_commit, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    //发送关闭pocRoom插件，关闭webrtc连接
                                    //JanusControl.janusControlDetach(FragmentMine.this,false);
                                    JanusControl.janusControlPocRoomDetach(FragmentMine.this);
                                    name = data.getUserFriendName();
                                    remoteId = data.getUserFriendId();
                                    isVideo = false;
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
    public void janusServer(int code, String msg) {
        switch (code){
            case 104:
                // pocroom onDetached
                Message message = new Message();
                message.what = 0;
                handler.sendMessage(message);
                break;
        }
    }

    private Handler handler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case 0:
                    Intent intent = new Intent(getActivity(), CallActivity.class);
                    intent.putExtra("isCall",true);
                    intent.putExtra("isVideo",isVideo);
                    intent.putExtra("name",name);
                    intent.putExtra("remoteId",remoteId);
                    getActivity().startActivity(intent);
                    break;
            }
        }
    };

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
