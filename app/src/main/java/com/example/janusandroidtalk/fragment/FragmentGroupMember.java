package com.example.janusandroidtalk.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
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

import com.example.janusandroidtalk.MainActivity;
import com.example.janusandroidtalk.R;
import com.example.janusandroidtalk.activity.GroupCreateActivity;
import com.example.janusandroidtalk.bean.UserBean;
import com.example.janusandroidtalk.bean.UserFriendBean;
import com.example.janusandroidtalk.bean.UserGroupBean;
import com.example.janusandroidtalk.dialog.CustomProgressDialog;
import com.example.janusandroidtalk.grpcconnectionmanager.GrpcConnectionManager;
import com.example.janusandroidtalk.pullrecyclerview.BaseRecyclerAdapter;
import com.example.janusandroidtalk.pullrecyclerview.BaseViewHolder;
import com.example.janusandroidtalk.pullrecyclerview.PullRecyclerView;
import com.example.janusandroidtalk.pullrecyclerview.layoutmanager.XLinearLayoutManager;

import java.util.ArrayList;
import java.util.List;

import talk_cloud.TalkCloudApp;

import static android.app.Activity.RESULT_OK;


public class FragmentGroupMember extends Fragment{
    private Dialog loading;
    private ImageView toolbarBack;
    private TextView setting;
    private TextView title;

    // Pulling down for refresh
    private PullRecyclerView mPullRecyclerView;

    // group member list adapter
    private GroupMemberListAdapter groupMemberListAdapter;

    private int groupPosition = 0;
    private int deleteMemberPosition = 0;
    private UserGroupBean currentGroup = null;
    private List<UserFriendBean> groupMemberList = new ArrayList<>();

    public static FragmentGroupMember newInstance() {
        FragmentGroupMember fragmentGroupMember = new FragmentGroupMember();
        return fragmentGroupMember;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_group_member_list, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        toolbarBack = (ImageView) getActivity().findViewById(R.id.fragment_group_member_list_back);
        setting = (TextView) getActivity().findViewById(R.id.fragment_group_member_list_menu);
        title = (TextView) getActivity().findViewById(R.id.fragment_group_member_list_title);
        groupPosition = getActivity().getIntent().getIntExtra("groupPosition",0);

        //赋值用户好友列表
        if (UserBean.getUserBean() != null) {
            currentGroup = UserBean.getUserBean().getUserGroupBeanArrayList().get(groupPosition);
            groupMemberList = currentGroup.getUserFriendBeanArrayList();
            title.setText(currentGroup.getUserGroupName());

            title.setText(UserBean.getUserBean().getUserGroupBeanArrayList().get(groupPosition).getUserGroupName());
        }

        //网络请求加载框
        loading = CustomProgressDialog.createLoadingDialog(getContext(), R.string.recycler_pull_loading);
        loading.setCancelable(true);
        loading.setCanceledOnTouchOutside(false);

        mPullRecyclerView = getActivity().findViewById(R.id.fragment_group_member_list_view);
        mPullRecyclerView.setLayoutManager(new XLinearLayoutManager(getActivity()));     // 设置LayoutManager
        mPullRecyclerView.setColorSchemeResources(R.color.colorMain);                    // 设置下拉刷新的旋转圆圈的颜色
        groupMemberListAdapter = new GroupMemberListAdapter(getActivity(), R.layout.activity_group_member_list_item, groupMemberList);
        mPullRecyclerView.setAdapter(groupMemberListAdapter);

        //第一次进来发起请求
        if (UserBean.getUserBean() != null) {
            mPullRecyclerView.postRefreshing();
            currentGroup = UserBean.getUserBean().getUserGroupBeanArrayList().get(groupPosition);
            groupMemberList = currentGroup.getUserFriendBeanArrayList();
            groupMemberListAdapter.notifyDataSetChanged();
            mPullRecyclerView.stopRefresh();
        }

        mPullRecyclerView.setOnRecyclerRefreshListener(new PullRecyclerView.OnRecyclerRefreshListener() {
            @Override
            public void onPullRefresh() {
                // 下拉刷新事件被触发
                if (UserBean.getUserBean() != null) {
                    currentGroup = UserBean.getUserBean().getUserGroupBeanArrayList().get(groupPosition);
                    groupMemberList = currentGroup.getUserFriendBeanArrayList();
                    groupMemberListAdapter.notifyDataSetChanged();
                    mPullRecyclerView.stopRefresh();
                }
            }

            @Override
            public void onLoadMore() {
                // 上拉加载更多事件被触发
            }
        });

        //进入groupCreateActivity 进行添加成员进群
        setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), GroupCreateActivity.class);
                intent.putExtra("groupPosition", groupPosition);
                intent.putExtra("addFlag",1);
                startActivityForResult(intent,1000);
            }
        });

        toolbarBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), MainActivity.class);
                startActivity(intent);
            }
        });
    }

    class GroupMemberListAdapter extends BaseRecyclerAdapter {
        private Context context;

        public GroupMemberListAdapter (Context context, int layoutResId, List<UserFriendBean> memberList) {
            super(context, layoutResId, memberList);
            this.context = context;
        }

        @Override
        protected void converted(BaseViewHolder holder, Object item, int position) {
            final UserFriendBean data = groupMemberList.get(position);

            ImageView imageView_portrait = holder.getView(R.id.group_member_list_item_image);       // Portrait
            holder.setText(R.id.group_member_list_item_name, data.getUserFriendName() + "");  // Name
            holder.setText(R.id.group_member_list_item_state, data.getUserFriendId() + "");   // ID
            ImageView imageView_deleter = holder.getView(R.id.group_member_list_item_delete);       // Deleter

//            LinearLayout linearLayout = holder.getView(R.layout.activity_group_member_list_item);   // TODO Add onClickListener later

            if (data.getOnline() == 2) {
                imageView_portrait.setImageResource(R.drawable.ic_group_member_portrait_black_24dp);
            } else {
                imageView_portrait.setImageResource(R.drawable.ic_group_member_portrait_gray_24dp);
            }

            if (currentGroup.getGroupManagerId() == UserBean.getUserBean().getUserId()) {
                imageView_deleter.setEnabled(true);
                imageView_deleter.setImageResource(R.drawable.ic_delete_black_24dp);
            } else {
                imageView_deleter.setEnabled(false);
                imageView_deleter.setImageResource(R.drawable.ic_delete_gray_24dp);
            }

            imageView_deleter.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    deleteMemberPosition = position;
                    deleteGroupMember(groupMemberList.get(position).getUserFriendId());
                }
            });
        }
    }

    public void deleteGroupMember(int deleteUserId){
        final AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setMessage(R.string.group_member_delete_member_tips)
                .setPositiveButton(R.string.dialog_commit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        //Todo 调用删除群组成员接口
//                        groupDeleteMember(deleteUserId);
                        Toast.makeText(getContext(), "do in back", Toast.LENGTH_SHORT).show();
                        handleGroupDeleteMemberBack(deleteUserId);
                        Toast.makeText(getContext(), "done", Toast.LENGTH_SHORT).show();
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

    // 删除群组成员
    // Delete group member thread
    public void handleGroupDeleteMemberBack(int deleteUserId) {
        int gid = UserBean.getUserBean().getUserGroupBeanArrayList().get(groupPosition).getUserGroupId();
        TalkCloudApp.GrpUserDelReq grpUserDelReq = TalkCloudApp.GrpUserDelReq.newBuilder().setGid(gid).setUid(deleteUserId).build();

        try {
            GrpcConnectionManager.getInstance().getGrpcInstantRequestHandler().submit(new Runnable() {
                @Override
                public void run() {
                    TalkCloudApp.GrpUserDelRsp grpUserDelRsp = GrpcConnectionManager.getInstance().getBlockingStub().removeGrpUser(grpUserDelReq);

                    Message msg = Message.obtain();
                    msg.obj = grpUserDelRsp;
                    msg.what = 100;
                    handler.sendMessage(msg);
                }
            });
        } catch (Exception e) {

        }
    }

    public void groupDeleteMember(TalkCloudApp.GrpUserDelRsp grpUserDelRsp) {
        loading.dismiss();

        if (grpUserDelRsp == null) {
            Toast.makeText(getContext(), R.string.request_data_null_tips, Toast.LENGTH_SHORT).show();
            return;
        }
        //判断result code
        if (grpUserDelRsp.getRes().getCode() != 200) {
            Toast.makeText(getContext(), grpUserDelRsp.getRes().getMsg(), Toast.LENGTH_SHORT).show();
            return;
        } else {
            //Todo 删除成功之后，将本地的数据删除，
            UserBean.getUserBean().getUserGroupBeanArrayList().get(groupPosition).getUserFriendBeanArrayList().remove(deleteMemberPosition);
            currentGroup = UserBean.getUserBean().getUserGroupBeanArrayList().get(groupPosition);
            groupMemberList = currentGroup.getUserFriendBeanArrayList();
            groupMemberListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == RESULT_OK && requestCode == 1000){
            Toast.makeText(getContext(), "onActivityResult", Toast.LENGTH_SHORT).show();
            currentGroup = UserBean.getUserBean().getUserGroupBeanArrayList().get(groupPosition);
            groupMemberList.clear();
            groupMemberList.addAll(currentGroup.getUserFriendBeanArrayList());
            groupMemberListAdapter.notifyDataSetChanged();
        }
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {      //判断标志位
                case 100:
                    groupDeleteMember((TalkCloudApp.GrpUserDelRsp)msg.obj);
                    break;
            }
        }
    };
}
