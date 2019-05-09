package com.example.janusandroidtalk.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.janusandroidtalk.MyApplication;
import com.example.janusandroidtalk.R;
import com.example.janusandroidtalk.bean.UserBean;
import com.example.janusandroidtalk.bean.UserFriendBean;
import com.example.janusandroidtalk.bean.UserGroupBean;
import com.example.janusandroidtalk.dialog.CustomProgressDialog;
import com.example.janusandroidtalk.floatwindow.FloatActionController;
import com.example.janusandroidtalk.grpcconnectionmanager.GrpcConnectionManager;
import com.example.janusandroidtalk.signalingcontrol.JanusControl;
import com.example.janusandroidtalk.signalingcontrol.MyControlCallBack;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.MediaStream;

import java.util.ArrayList;

import talk_cloud.TalkCloudApp;

public class GroupCreateActivity extends AppCompatActivity implements MyControlCallBack {
    private EditText editSearch;
    private Dialog loading;
    private TextView menu;
    private TextView title;
    private ImageView toolbarBack = null;

    private ArrayList<UserFriendBean> myFriendsList = new ArrayList<>();

    private ListView listView;
    private GroupListAdapter groupListAdapter;

    private int sum = 0;
    private String userIds = "";

    private int groupPosition = 0;        //当前群组
    private int addFlag = 0;              //0为创建群组，1为添加成员，
    private UserGroupBean currentGroup;  //当前群组对象，

    // 当前群组已添加成员列表
    private ArrayList<UserFriendBean> addedList = new ArrayList<>();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);
        editSearch = (EditText) findViewById(R.id.create_group_edit);
        toolbarBack = (ImageView) findViewById(R.id.create_group_back);
        listView = (ListView) findViewById(R.id.create_group_list_view);
        menu = (TextView) findViewById(R.id.create_group_menu);
        title = (TextView) findViewById(R.id.create_group_title);

        groupPosition = getIntent().getIntExtra("groupPosition",0);
        addFlag = getIntent().getIntExtra("addFlag",0);

        //赋值用户好友列表
        if (UserBean.getUserBean() != null && addFlag == 0 ) {
            //创建群组
            myFriendsList = UserBean.getUserBean().getUserFriendBeanArrayList();
        }else if(UserBean.getUserBean() != null && addFlag == 1 ){
            //获取当前群组的成员列表，并将已在群组的好友设置为已经选择
            currentGroup = UserBean.getUserBean().getUserGroupBeanArrayList().get(groupPosition);
            myFriendsList = UserBean.getUserBean().getUserFriendBeanArrayList();
            for (UserFriendBean userFriendBean: myFriendsList) {
                for(UserFriendBean userFriendBeanMember: currentGroup.getUserFriendBeanArrayList()){
                    if(userFriendBean.getUserFriendId() == userFriendBeanMember.getUserFriendId()){
                        userFriendBean.setInGroup(true);
                        addedList.add(userFriendBean);
                    }
                }
            }
            //获取当前群组名称，并设置为title
            title.setText(R.string.group_member_list_title_menu);
        }

        loading = CustomProgressDialog.createLoadingDialog(this, R.string.recycler_pull_loading);
        loading.setCancelable(true);
        loading.setCanceledOnTouchOutside(false);

        //隐藏悬浮窗
        FloatActionController.getInstance().hide();

        //返回事件,清空本次选择的成员
        toolbarBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (UserFriendBean userFriendBean: myFriendsList) {
                    userFriendBean.setCheck(false);
                }
                GroupCreateActivity.this.finish();
            }
        });

        groupListAdapter = new GroupListAdapter();
        listView.setAdapter(groupListAdapter);
        listView.setTextFilterEnabled(true);

        if(UserBean.getUserBean() != null) {
            userIds = UserBean.getUserBean().getUserId() + "";
        }

        menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               if(addFlag == 0){
                   final EditText editText = new EditText(GroupCreateActivity.this);
                   final AlertDialog dialog = new AlertDialog.Builder(GroupCreateActivity.this)
                           .setTitle(R.string.dialog_message_group_name)
                           .setView(editText)
                           .setPositiveButton(R.string.dialog_commit, new DialogInterface.OnClickListener() {
                               @Override
                               public void onClick(DialogInterface dialog, int which) {
                                   if(!TextUtils.isEmpty(editText.getText().toString())){
                                       dialog.dismiss();
                                       for (UserFriendBean userFriendBean: myFriendsList) {
                                           if(userFriendBean.isCheck()){
                                               userIds = userIds + "," + userFriendBean.getUserFriendId();
                                           }
                                       }
                                       handleCreateGroupBack(userIds, editText.getText().toString());
                                   }
                               }
                           })
                           .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                               @Override
                               public void onClick(DialogInterface dialog, int which) {
                                   dialog.dismiss();
                               }
                           }).create();
                   dialog.show();
               }else{
                   final AlertDialog dialog = new AlertDialog.Builder(GroupCreateActivity.this)
                           .setMessage(R.string.dialog_message_group_add_member)
                           .setPositiveButton(R.string.dialog_commit, new DialogInterface.OnClickListener() {
                               @Override
                               public void onClick(DialogInterface dialog, int which) {
                                   dialog.dismiss();
                                   for (UserFriendBean userFriendBean: myFriendsList) {
                                       if(userFriendBean.isCheck()){
                                           userIds = userIds + "," + userFriendBean.getUserFriendId();
                                       }
                                   }
                                   handleAddGroupMemberBack(userIds);
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
            }
        });

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
                // When user change the text
                groupListAdapter.getFilter().filter(cs);
            }

            @Override
            public void beforeTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
                //
            }

            @Override
            public void afterTextChanged(Editable arg0) {
                //
            }
        });
    }

    //群组列表adapter
    class GroupListAdapter extends BaseAdapter implements Filterable {

        //总行数
        @Override
        public int getCount() {
            return myFriendsList.size();
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
                convertView = LayoutInflater.from(GroupCreateActivity.this).inflate(R.layout.activity_create_group_list_item, parent, false);
                holder = new ViewHolder();
                holder.imageView = (ImageView) convertView.findViewById(R.id.create_group_list_item_image);
                holder.nameTextView = (TextView) convertView.findViewById(R.id.create_group_list_item_name);
                holder.stateTextView = (TextView) convertView.findViewById(R.id.create_group_list_item_state);
                holder.checkBox = (CheckBox) convertView.findViewById(R.id.create_group_list_item_checkbox);
                holder.textViewInGroup = (TextView) convertView.findViewById(R.id.create_group_list_item_text);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.nameTextView.setText(myFriendsList.get(position).getUserFriendName());
            holder.stateTextView.setText(myFriendsList.get(position).getUserFriendId() + "");
            if(addFlag == 1){
                for (UserFriendBean userFriendBean: addedList) {
                    if (userFriendBean.getUserFriendId() == myFriendsList.get(position).getUserFriendId()) {
                        holder.checkBox.setVisibility(View.GONE);
                        holder.textViewInGroup.setVisibility(View.VISIBLE);
                        holder.textViewInGroup.setText(R.string.search_str_added_group);
                    }
                }
            }else{
                holder.checkBox.setVisibility(View.VISIBLE);
                holder.textViewInGroup.setVisibility(View.GONE);
            }

            holder.checkBox.setOnCheckedChangeListener(null);
            holder.checkBox.setChecked(myFriendsList.get(position).isCheck());
            holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    String sure = getResources().getString(R.string.toolbar_commit);
                    if (isChecked) {
                        myFriendsList.get(position).setCheck(true);
                        sum = sum + 1;
                        menu.setText(sure+"(" + sum + ")");
                    } else {
                        sum = sum - 1;
                        if (sum == 0) {
                            menu.setText(R.string.toolbar_commit);
                        } else {
                            menu.setText(sure+"(" + sum + ")");
                        }
                        myFriendsList.get(position).setCheck(false);
                    }
                }
            });

            return convertView;
        }

        private ArrayFilter mFilter;
        private ArrayList<UserFriendBean> mOriginalValues;
        private final Object mLock = new Object();

        @Override
        public Filter getFilter() {
            if (mFilter == null) {
                mFilter = new ArrayFilter();
            }
            return mFilter;
        }

        private class ArrayFilter extends Filter {
            @Override
            protected FilterResults performFiltering(CharSequence prefix) {
                FilterResults results = new FilterResults();

                if (mOriginalValues == null) {
                    synchronized (mLock) {
                        mOriginalValues = new ArrayList<UserFriendBean>(myFriendsList);
                    }
                }

                if (prefix == null || prefix.length() == 0) {
                    ArrayList<UserFriendBean> list;
                    synchronized (mLock) {
                        list = new ArrayList<UserFriendBean>(mOriginalValues);
                    }
                    results.values = list;
                    results.count = list.size();
                } else {
                    String prefixString = prefix.toString().toLowerCase();

                    ArrayList<UserFriendBean> values;
                    synchronized (mLock) {
                        values = new ArrayList<UserFriendBean>(mOriginalValues);
                    }

                    final int count = values.size();
                    final ArrayList<UserFriendBean> newValues = new ArrayList<UserFriendBean>();

                    for (int i = 0; i < count; i++) {
                        final UserFriendBean value = values.get(i);
                        final String valueText = value.getUserFriendName().toString().toLowerCase();

                        // First match against the whole, non-splitted value
                        if (valueText.contains(prefixString)) {
                            newValues.add(value);
                        } else {
                            final String[] words = valueText.split(" ");
                            final int wordCount = words.length;

                            // Start at index 0, in case valueText starts with space(s)
                            for (int k = 0; k < wordCount; k++) {
                                if (words[k].contains(prefixString)) {
                                    newValues.add(value);
                                    break;
                                }
                            }
                        }
                    }

                    results.values = newValues;
                    results.count = newValues.size();
                }

                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                //noinspection unchecked
                myFriendsList = (ArrayList<UserFriendBean>) results.values;
                if (results.count > 0) {
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }
        }

        class ViewHolder {
            ImageView imageView;
            TextView nameTextView;
            TextView stateTextView;
            CheckBox checkBox;
            TextView textViewInGroup;
        }
    }

    //添加群组请求
    public void handleCreateGroupBack(String userIds, String groupName) {
        loading.show();

        int accountId = 0;
        if(UserBean.getUserBean() != null){
            accountId = UserBean.getUserBean().getUserId();
        }

        TalkCloudApp.CreateGroupReq createGroupReq = TalkCloudApp.CreateGroupReq.newBuilder().setDeviceIds(userIds).setGroupName(groupName).setAccountId(accountId).build();

        try {
            GrpcConnectionManager.getInstance().getGrpcInstantRequestHandler().submit(new Runnable() {
                @Override
                public void run() {
                    TalkCloudApp.CreateGroupResp createGroupResp = GrpcConnectionManager.getInstance().getBlockingStub().createGroup(createGroupReq);

                    Message msg = Message.obtain();
                    msg.obj = createGroupResp;
                    msg.what = 200;
                    handler.sendMessage(msg);
                }
            });
        } catch (Exception e) {

        }
    }

    public void createGroup(TalkCloudApp.CreateGroupResp createGroupResp) {
        loading.dismiss();

        if (createGroupResp == null) {
            Toast.makeText(GroupCreateActivity.this, R.string.request_data_null_tips, Toast.LENGTH_SHORT).show();
            return;
        }

        //判断createGroupResp code
        if (createGroupResp.getRes().getCode() != 200) {
            Toast.makeText(GroupCreateActivity.this, createGroupResp.getRes().getMsg(), Toast.LENGTH_SHORT).show();
            return;
        } else {
            //创建成功
            JanusControl.sendCreateGroup(GroupCreateActivity.this, (int)(createGroupResp.getGroupInfo().getGid()));

            //初始化新加群成员
            UserGroupBean newGroup = new UserGroupBean();
            newGroup.setUserGroupBeanObj(createGroupResp.getGroupInfo());
            UserBean.getUserBean().getUserGroupBeanArrayList().add(newGroup);

            //判断是否存在默认进入的群组id，如果没有则默认第一个群组id
            if(MyApplication.getDefaultGroupId() == 0 && UserBean.getUserBean().getUserGroupBeanArrayList().size() > 0){
                MyApplication.setDefaultGroupId(UserBean.getUserBean().getUserGroupBeanArrayList().get(0).getUserGroupId());
            }

            setResult(RESULT_OK);
            GroupCreateActivity.this.finish();
        }
    }

    //添加好友进群
    public void handleAddGroupMemberBack(String userIds) {
        loading.show();

        int gid = currentGroup.getUserGroupId();
        TalkCloudApp.InviteUserReq inviteUserReq = TalkCloudApp.InviteUserReq.newBuilder().setGid(gid).setUids(userIds).build();

        try {
            GrpcConnectionManager.getInstance().getGrpcInstantRequestHandler().submit(new Runnable() {
                @Override
                public void run() {
                    loading.dismiss();
                    TalkCloudApp.InviteUserResp inviteUserResp = GrpcConnectionManager.getInstance().getBlockingStub().inviteUserIntoGroup(inviteUserReq);

                    Message msg = Message.obtain();
                    msg.obj = inviteUserResp;
                    msg.what = 100;
                    handler.sendMessage(msg);
                }
            });
        } catch (Exception e) {

        }
    }

    public void addGroupMember(TalkCloudApp.InviteUserResp inviteUserResp) {
        loading.dismiss();

        if (inviteUserResp == null) {
            Toast.makeText(GroupCreateActivity.this, R.string.request_data_null_tips, Toast.LENGTH_SHORT).show();
            return;
        }
        //判断inviteUserResp code
        if (inviteUserResp.getRes().getCode() != 200) {
            Toast.makeText(GroupCreateActivity.this, inviteUserResp.getRes().getMsg(), Toast.LENGTH_SHORT).show();
            return;
        } else {
            //添加成功，
            Toast.makeText(GroupCreateActivity.this, R.string.new_group_str_add_member_success, Toast.LENGTH_SHORT).show();

            // 更新当前群成员列表
            // Updating the Group's UserFriendBeanArrayList
            ArrayList<UserFriendBean> newGroupMemberList = new ArrayList<UserFriendBean>();
            for (TalkCloudApp.UserRecord newGroupMember : inviteUserResp.getUsrListList()) {
                Log.d("", "------------------------>" + newGroupMember.getOnline());
                UserFriendBean userFriendBean = new UserFriendBean();
                // FIXME
                userFriendBean.setUserFriendId(newGroupMember.getUid());
                userFriendBean.setUserFriendName(newGroupMember.getName());
                userFriendBean.setOnline(newGroupMember.getOnline());

                newGroupMemberList.add(userFriendBean);
            }

            UserBean.getUserBean().getUserGroupBeanArrayList().get(groupPosition).setUserFriendBeanArrayList(newGroupMemberList);

            //状态重置
            for (UserFriendBean userFriendBean: myFriendsList) {
                userFriendBean.setCheck(false);
                userFriendBean.setInGroup(false);
            }
            setResult(RESULT_OK);
            GroupCreateActivity.this.finish();
        }
    }

    @Override
    public void janusServer(int code,String msg) {

    }

    //回调信息处理，
    @Override
    public void showMessage(final JSONObject msg,JSONObject jsepLocal) {
        try{
            if(msg.getString("pocroom").equals("created")) {
                Message message = new Message();
                message.what = 1;
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
                    Toast.makeText(GroupCreateActivity.this, R.string.new_group_str_create_success, Toast.LENGTH_SHORT).show();
                    for (UserFriendBean userFriendBean: myFriendsList) {
                        userFriendBean.setCheck(false);
                    }
                    GroupCreateActivity.this.finish();
                    break;
                case 100:
                    Toast.makeText(GroupCreateActivity.this, "invite friends ok", Toast.LENGTH_SHORT).show();
                    addGroupMember((TalkCloudApp.InviteUserResp) msg.obj);
                    break;
                case 200:
                    Toast.makeText(GroupCreateActivity.this, "create group ok", Toast.LENGTH_SHORT).show();
                    createGroup((TalkCloudApp.CreateGroupResp)msg.obj);
                    break;
            }
        };
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            for (UserFriendBean userFriendBean: myFriendsList) {
                userFriendBean.setCheck(false);
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
