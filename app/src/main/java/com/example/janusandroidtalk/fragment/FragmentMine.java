package com.example.janusandroidtalk.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
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
import com.example.janusandroidtalk.pullrecyclerview.BaseRecyclerAdapter;
import com.example.janusandroidtalk.pullrecyclerview.BaseViewHolder;
import com.example.janusandroidtalk.pullrecyclerview.PullRecyclerView;
import com.example.janusandroidtalk.pullrecyclerview.layoutmanager.XLinearLayoutManager;
import com.example.janusandroidtalk.signalingcontrol.JanusControl;
import com.example.janusandroidtalk.tools.AppTools;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import talk_cloud.TalkCloudApp;
import talk_cloud.TalkCloudGrpc;

public class FragmentMine extends Fragment{

    private PullRecyclerView mPullRecyclerView;
    private List<UserFriendBean> myList = new ArrayList<>();
    private MineListAdapter mAdapter;

    private LinearLayout searchView;
    private TextView imageView;

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
            new GrpcGetFriendListTask().execute(UserBean.getUserBean().getUserId() + "");
        }
        //下拉刷新
        mPullRecyclerView.setOnRecyclerRefreshListener(new PullRecyclerView.OnRecyclerRefreshListener() {
            @Override
            public void onPullRefresh() {
                // 下拉刷新事件被触发
                if (UserBean.getUserBean() != null) {
                    new GrpcGetFriendListTask().execute(UserBean.getUserBean().getUserId() + "");
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
            ImageView imageView = holder.getView(R.id.fragment_mine_list_item_audio_call);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(context, CallActivity.class);
                    intent.putExtra("isCall",true);
                    intent.putExtra("name","xiaozhuzhu");
                    context.startActivity(intent);
                }
            });

        }
    }

    class GrpcGetFriendListTask extends AsyncTask<String, Void, TalkCloudApp.FriendsRsp> {
        private ManagedChannel channel;

        private GrpcGetFriendListTask() {

        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected TalkCloudApp.FriendsRsp doInBackground(String... params) {
            int id = Integer.parseInt(params[0]);
            TalkCloudApp.FriendsRsp replay = null;
            try {
                channel = ManagedChannelBuilder.forAddress(AppTools.host, AppTools.port).usePlaintext().build();
                TalkCloudGrpc.TalkCloudBlockingStub stub = TalkCloudGrpc.newBlockingStub(channel);
                TalkCloudApp.FriendsReq regReq = TalkCloudApp.FriendsReq.newBuilder().setUid(id).build();
                replay = stub.getFriendList(regReq);
                return replay;
            } catch (Exception e) {
                return replay;
            }
        }

        @Override
        protected void onPostExecute(TalkCloudApp.FriendsRsp result) {
            try {
                channel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            mPullRecyclerView.stopRefresh();
            if(result == null){
                Toast.makeText(getContext(),R.string.request_data_null_tips,Toast.LENGTH_SHORT).show();
                return;
            }
            //判断result code
            if(result.getRes().getCode() != 200){
                Toast.makeText(getContext(),result.getRes().getMsg(),Toast.LENGTH_SHORT).show();
                return;
            }

            myList.clear();
            ArrayList<UserFriendBean> userFriendBeanArrayList = new ArrayList<UserFriendBean>();
            for (TalkCloudApp.FriendRecord friendRecord : result.getFriendListList()) {
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
    }


}
