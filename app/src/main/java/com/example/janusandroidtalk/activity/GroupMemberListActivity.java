package com.example.janusandroidtalk.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.janusandroidtalk.R;
import com.example.janusandroidtalk.bean.UserBean;
import com.example.janusandroidtalk.bean.UserFriendBean;
import com.example.janusandroidtalk.bean.UserGroupBean;
import com.example.janusandroidtalk.dialog.CustomProgressDialog;
import com.example.janusandroidtalk.grpcconnectionmanager.GrpcConnectionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import talk_cloud.TalkCloudApp;

public class GroupMemberListActivity extends AppCompatActivity{

    private Dialog loading;
    private TextView menu;

    private List<UserFriendBean> myList = new ArrayList<>();
    private ImageView toolbarBack = null;
    private ListView listView;
    private TextView title;

    private GroupMemberListAdapter groupListAdapter;

    private int groupPosition = 0;
    private UserGroupBean userGroupBean = null;

    private int deletePosition = 0;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_member_list);
        toolbarBack = (ImageView) findViewById(R.id.group_member_list_back);
        listView = (ListView) findViewById(R.id.group_member_list_view);
        menu = (TextView) findViewById(R.id.group_member_list_menu);
        title = (TextView) findViewById(R.id.group_member_list_title);

        groupPosition = getIntent().getIntExtra("groupPosition",0);

        //赋值用户好友列表
        if (UserBean.getUserBean() != null) {
            userGroupBean = UserBean.getUserBean().getUserGroupBeanArrayList().get(groupPosition);
            myList = userGroupBean.getUserFriendBeanArrayList();
            title.setText(userGroupBean.getUserGroupName());
        }

        //网络请求加载框
        loading = CustomProgressDialog.createLoadingDialog(this, R.string.recycler_pull_loading);
        loading.setCancelable(true);
        loading.setCanceledOnTouchOutside(false);

        //隐藏悬浮窗
        //FloatActionController.getInstance().hide();

        //返回事件
        toolbarBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GroupMemberListActivity.this.finish();
            }
        });

        groupListAdapter = new GroupMemberListAdapter();
        listView.setAdapter(groupListAdapter);
        listView.setTextFilterEnabled(true);

        //进入groupcreateActivity 进行添加成员进群
        menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GroupMemberListActivity.this, GroupCreateActivity.class);
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
            ViewHolder holder = null;
            if (convertView == null) {
                convertView = LayoutInflater.from(GroupMemberListActivity.this).inflate(R.layout.activity_group_member_list_item, parent, false);
                holder = new ViewHolder();
                holder.imageView = (ImageView) convertView.findViewById(R.id.group_member_list_item_image);
                holder.nameTextView = (TextView) convertView.findViewById(R.id.group_member_list_item_name);
                holder.stateTextView = (TextView) convertView.findViewById(R.id.group_member_list_item_state);
                holder.imageViewDelete = (ImageView) convertView.findViewById(R.id.group_member_list_item_delete);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
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


            holder.imageViewDelete.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    deletePosition = position;
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
        final AlertDialog dialog = new AlertDialog.Builder(GroupMemberListActivity.this)
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
            Toast.makeText(GroupMemberListActivity.this, R.string.request_data_null_tips, Toast.LENGTH_SHORT).show();
            return;
        }
        //判断result code
        if (grpUserDelRsp.getRes().getCode() != 200) {
            Toast.makeText(GroupMemberListActivity.this, grpUserDelRsp.getRes().getMsg(), Toast.LENGTH_SHORT).show();
            return;
        } else {
            //Todo 删除成功之后，将本地的数据删除，
            UserBean.getUserBean().getUserGroupBeanArrayList().get(groupPosition).getUserFriendBeanArrayList().remove(deletePosition);
            userGroupBean = UserBean.getUserBean().getUserGroupBeanArrayList().get(groupPosition);
            myList = userGroupBean.getUserFriendBeanArrayList();
            groupListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
       if(resultCode == RESULT_OK && requestCode == 1000){
            userGroupBean = UserBean.getUserBean().getUserGroupBeanArrayList().get(groupPosition);
            myList = userGroupBean.getUserFriendBeanArrayList();
            groupListAdapter.notifyDataSetChanged();
       }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        GroupMemberListActivity.this.finish();
        return super.onKeyDown(keyCode, event);
    }
}
