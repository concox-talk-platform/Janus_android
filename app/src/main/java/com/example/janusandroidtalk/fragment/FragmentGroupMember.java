package com.example.janusandroidtalk.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.janusandroidtalk.R;
import com.example.janusandroidtalk.activity.GroupActivity;
import com.example.janusandroidtalk.activity.GroupCreateActivity;
import com.example.janusandroidtalk.activity.GroupMemberListActivity;
import com.example.janusandroidtalk.bean.UserBean;
import com.example.janusandroidtalk.bean.UserFriendBean;
import com.example.janusandroidtalk.bean.UserGroupBean;
import com.example.janusandroidtalk.dialog.CustomProgressDialog;
import com.example.janusandroidtalk.grpcconnectionmanager.GrpcConnectionManager;
import com.example.janusandroidtalk.pullrecyclerview.PullRecyclerView;
import com.example.janusandroidtalk.pullrecyclerview.layoutmanager.XLinearLayoutManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import talk_cloud.TalkCloudApp;

import static android.app.Activity.RESULT_OK;


public class FragmentGroupMember extends Fragment{
    private Dialog loading;
    private TextView setting;

    private List<UserFriendBean> myList = new ArrayList<>();
    private ListView listView;
    private TextView title;

    // Pulling down for refresh
    private PullRecyclerView mPullRecyclerView;

    // Layout adapter
    private GroupMemberListAdapter groupMemberListAdapter;

    private int groupPosition = 0;
    private int deleteMemberPosition = 0;

    private UserGroupBean userGroupBean = null;

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
        return inflater.inflate(R.layout.fagment_group_member, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setting = (TextView) getActivity().findViewById(R.id.group_member_list_menu2);
        mPullRecyclerView = getActivity().findViewById(R.id.group_list);
        title = (TextView) getActivity().findViewById(R.id.group_member_list_title2);
        groupPosition = getActivity().getIntent().getIntExtra("groupPosition",0);
        listView = (ListView) getActivity().findViewById(R.id.group_member_list_view2);

        //赋值用户好友列表
        if (UserBean.getUserBean() != null) {
            userGroupBean = UserBean.getUserBean().getUserGroupBeanArrayList().get(groupPosition);
            myList = userGroupBean.getUserFriendBeanArrayList();
            title.setText(userGroupBean.getUserGroupName());
        }

        //网络请求加载框
        loading = CustomProgressDialog.createLoadingDialog(getContext(), R.string.recycler_pull_loading);
        loading.setCancelable(true);
        loading.setCanceledOnTouchOutside(false);

        groupMemberListAdapter = new GroupMemberListAdapter();
        listView.setAdapter(groupMemberListAdapter);
        listView.setTextFilterEnabled(true);

        //进入groupcreateActivity 进行添加成员进群
        setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), GroupCreateActivity.class);
                intent.putExtra("groupPosition", groupPosition);
                intent.putExtra("addFlag",1);
                startActivityForResult(intent,1000);
            }
        });
    }

    //群组列表adapter
    class GroupMemberListAdapter extends BaseAdapter {

        //总行数
        @Override
        public int getCount() {
            return myList.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            GroupMemberListAdapter.ViewHolder holder = null;
            if (convertView == null) {
                convertView = LayoutInflater.from(getActivity()).inflate(R.layout.activity_group_member_list_item, parent, false);
                holder = new GroupMemberListAdapter.ViewHolder();
                holder.imageView = (ImageView) convertView.findViewById(R.id.group_member_list_item_image);
                holder.nameTextView = (TextView) convertView.findViewById(R.id.group_member_list_item_name);
                holder.stateTextView = (TextView) convertView.findViewById(R.id.group_member_list_item_state);
                holder.imageViewDelete = (ImageView) convertView.findViewById(R.id.group_member_list_item_delete);
                convertView.setTag(holder);
            } else {
                holder = (GroupMemberListAdapter.ViewHolder) convertView.getTag();
            }

            // Only group manager can delete members, so show difference
            if(UserBean.getUserBean().getUserId() != userGroupBean.getGroupManagerId()){
                holder.imageViewDelete.setEnabled(false);
                holder.imageViewDelete.setImageResource(R.drawable.ic_delete_gray_24dp);
            }else{
                holder.imageViewDelete.setEnabled(true);
                holder.imageViewDelete.setImageResource(R.drawable.ic_delete_black_24dp);
            }

            holder.nameTextView.setText(myList.get(position).getUserFriendName());
            // Show online status
            if (myList.get(position).getOnline() == 2) {// Online
                holder.stateTextView.setText(myList.get(position).getUserFriendId() + " 在线");
            }
            else {
                holder.stateTextView.setText(myList.get(position).getUserFriendId() + " 离线");
            }

            //TODO Trouble here
            holder.imageViewDelete.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    deleteMemberPosition = position;
                    deleteGroupMember(myList.get(position).getUserFriendId());
                }
            });

            return convertView;
        }

        class ViewHolder {
            ImageView imageView;
            TextView nameTextView;
            TextView stateTextView;
            ImageView imageViewDelete;
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
                        groupDeleteMember(deleteUserId);
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
    public void groupDeleteMember(int deleteUserId) {
        int gid = UserBean.getUserBean().getUserGroupBeanArrayList().get(groupPosition).getUserGroupId();
        TalkCloudApp.GrpUserDelReq grpUserDelReq = TalkCloudApp.GrpUserDelReq.newBuilder().setGid(gid).setUid(deleteUserId).build();
        TalkCloudApp.GrpUserDelRsp grpUserDelRsp = null;
        try {
            Future<TalkCloudApp.GrpUserDelRsp> future = GrpcConnectionManager.getInstance().getGrpcInstantRequestHandler().submit(new Callable<TalkCloudApp.GrpUserDelRsp>() {
                @Override
                public TalkCloudApp.GrpUserDelRsp call() throws Exception {
                    return GrpcConnectionManager.getInstance().getBlockingStub().removeGrpUser(grpUserDelReq);
                }
            });

            grpUserDelRsp = future.get();
        } catch (Exception e) {
            //TODO Nothing here
        }

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
            userGroupBean = UserBean.getUserBean().getUserGroupBeanArrayList().get(groupPosition);
            myList = userGroupBean.getUserFriendBeanArrayList();
            groupMemberListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == RESULT_OK && requestCode == 1000){
            userGroupBean = UserBean.getUserBean().getUserGroupBeanArrayList().get(groupPosition);
            myList = userGroupBean.getUserFriendBeanArrayList();
            groupMemberListAdapter.notifyDataSetChanged();
        }
    }
}
