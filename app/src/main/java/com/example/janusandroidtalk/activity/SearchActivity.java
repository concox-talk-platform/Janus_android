package com.example.janusandroidtalk.activity;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.janusandroidtalk.R;
import com.example.janusandroidtalk.bean.UserBean;
import com.example.janusandroidtalk.dialog.CustomProgressDialog;
import com.example.janusandroidtalk.floatwindow.FloatActionController;
import com.example.janusandroidtalk.grpcconnectionmanager.GrpcConnectionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import talk_cloud.TalkCloudApp;

public class SearchActivity extends AppCompatActivity {

    private EditText editSearch;
    private Dialog loading;

    private List<Map<String, String>> myList = new ArrayList<>();
    private TextView toolbarTitle;
    private ImageView toolbarBack = null;
    private ListView listView;

    private SearchFriendListAdapter searchFriendListAdapter;
    private SearchGroupListAdapter searchGroupListAdapter;

    private int searchType = 0;
    private int myPosition = 0;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        editSearch = (EditText) findViewById(R.id.search_edit);
        toolbarTitle = (TextView) findViewById(R.id.search_title);
        toolbarBack = (ImageView) findViewById(R.id.search_back);
        listView = (ListView) findViewById(R.id.search_list_view);

        loading = CustomProgressDialog.createLoadingDialog(this,R.string.recycler_pull_loading);
        loading.setCancelable(true);
        loading.setCanceledOnTouchOutside(false);

        //隐藏悬浮窗
        FloatActionController.getInstance().hide();

        searchType = getIntent().getIntExtra("searchType",0);
        switch (searchType){
            case 0:
                toolbarTitle.setText(R.string.search_str_group);
                editSearch.setHint(R.string.search_str_group);
                break;
            case 1:
                toolbarTitle.setText(R.string.search_str_friend);
                editSearch.setHint(R.string.search_str_friend);
                break;
        }

        //返回事件
        toolbarBack.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                SearchActivity.this.finish();
            }
        });

        //搜索事件
        editSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    //doSearch();
                    if(TextUtils.isEmpty(editSearch.getText().toString())){
                        Toast.makeText(SearchActivity.this,R.string.login_empty_tips,Toast.LENGTH_SHORT).show();
                        return true;
                    }else{
                        //关闭软键盘
                        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                        View myView = getWindow().peekDecorView();
                        if (null != myView) {
                            imm.hideSoftInputFromWindow(myView.getWindowToken(), 0);
                        }
                        switch (searchType){
                            case 0:
                                //搜索群组
                                if (UserBean.getUserBean() != null) {
                                    searchGroupList();
                                }
                                break;
                            case 1:
                                //搜索好友
                                if (UserBean.getUserBean() != null) {
                                    searchFriendList();
                                }
                                break;
                        }
                    }
                    return true;
                }
                return false;
            }
        });
    }

    //搜索群组列表请求
    public void searchGroupList() {
        loading.show();

        int id = UserBean.getUserBean().getUserId();
        String target = editSearch.getText().toString();
        TalkCloudApp.GrpSearchReq grpSearchReq = TalkCloudApp.GrpSearchReq.newBuilder().setUid(id).setTarget(target).build();
        TalkCloudApp.GroupListRsp groupListRsp = null;
        try {
            Future<TalkCloudApp.GroupListRsp> future = GrpcConnectionManager.getInstance().getGrpcInstantRequestHandler().submit(new Callable<TalkCloudApp.GroupListRsp>() {
                @Override
                public TalkCloudApp.GroupListRsp call() throws Exception {
                    return GrpcConnectionManager.getInstance().getBlockingStub().searchGroup(grpSearchReq);
                }
            });

            groupListRsp = future.get();
        } catch (Exception e) {
            //TODO Nothing here
        }

        loading.dismiss();
        if(groupListRsp == null){
            Toast.makeText(SearchActivity.this, R.string.request_data_null_tips, Toast.LENGTH_SHORT).show();
            return;
        }
        //判断result code
        if(groupListRsp.getRes().getCode() != 200){
            Toast.makeText(SearchActivity.this, groupListRsp.getRes().getMsg(), Toast.LENGTH_SHORT).show();
            return;
        }
        //判断列表数据为空
        if(groupListRsp.getGroupListList().size() == 0){
            Toast.makeText(SearchActivity.this, R.string.request_data_null_tips, Toast.LENGTH_SHORT).show();
            return;
        }

        myList.clear();
        for (TalkCloudApp.GroupInfo groupInfo : groupListRsp.getGroupListList()) {
            Map<String,String> map1 = new HashMap<String,String>();
            map1.put("name",groupInfo.getGroupName());
            map1.put("gid",groupInfo.getGid()+"");
            if(groupInfo.getIsExist()){
                map1.put("isExist","1");
            }else{
                map1.put("isExist","0");
            }
            myList.add(map1);
        }

        searchGroupListAdapter = new SearchGroupListAdapter();
        listView.setAdapter(searchGroupListAdapter);
    }

    //搜索好友列表请求
    public void searchFriendList() {
        loading.show();

        int id = UserBean.getUserBean().getUserId();
        String target = editSearch.getText().toString();
        TalkCloudApp.UserSearchReq userSearchReq = TalkCloudApp.UserSearchReq.newBuilder().setUid(id).setTarget(target).build();
        TalkCloudApp.UserSearchRsp userSearchRsp = null;
        try {
            Future<TalkCloudApp.UserSearchRsp> future = GrpcConnectionManager.getInstance().getGrpcInstantRequestHandler().submit(new Callable<TalkCloudApp.UserSearchRsp>() {
                @Override
                public TalkCloudApp.UserSearchRsp call() throws Exception {
                    return GrpcConnectionManager.getInstance().getBlockingStub().searchUserByKey(userSearchReq);
                }
            });

            userSearchRsp = future.get();
        } catch (Exception e) {

        }

        loading.dismiss();
        if(userSearchRsp == null){
            Toast.makeText(SearchActivity.this, R.string.request_data_null_tips, Toast.LENGTH_SHORT).show();
            return;
        }
        //判断result code
        if(userSearchRsp.getRes().getCode() != 200){
            Toast.makeText(SearchActivity.this, userSearchRsp.getRes().getMsg(), Toast.LENGTH_SHORT).show();
            return;
        }

        //判断列表数据为空
        if(userSearchRsp.getUserListList().size() == 0){
            Toast.makeText(SearchActivity.this, R.string.request_data_null_tips, Toast.LENGTH_SHORT).show();
            return;
        }

        myList.clear();
        for (TalkCloudApp.UserRecord userRecord : userSearchRsp.getUserListList()) {
            Map<String,String> map1 = new HashMap<String,String>();
            map1.put("name",userRecord.getName());
            map1.put("uid",userRecord.getUid()+"");
            if(userRecord.getIsFriend()){
                map1.put("isFriend","1");
            }else{
                map1.put("isFriend","0");
            }

            myList.add(map1);
        }

        searchFriendListAdapter = new SearchFriendListAdapter();
        listView.setAdapter(searchFriendListAdapter);
    }

    //添加群组请求
    public void userAddGroup(String uidStr, String gidStr) {
        loading.show();

        int uid = Integer.parseInt(uidStr);
        int gid = Integer.parseInt(gidStr);
        TalkCloudApp.GrpUserAddReq grpUserAddReq = TalkCloudApp.GrpUserAddReq.newBuilder().setUid(uid).setGid(gid).build();
        TalkCloudApp.GrpUserAddRsp grpUserAddRsp = null;
        try {
            Future<TalkCloudApp.GrpUserAddRsp> future = GrpcConnectionManager.getInstance().getGrpcInstantRequestHandler().submit(new Callable<TalkCloudApp.GrpUserAddRsp>() {
                @Override
                public TalkCloudApp.GrpUserAddRsp call() throws Exception {
                    return GrpcConnectionManager.getInstance().getBlockingStub().joinGroup(grpUserAddReq);
                }
            });

            grpUserAddRsp = future.get();
        } catch (Exception e) {
            //TODO Nothing here
        }

        loading.dismiss();

        if(grpUserAddRsp == null){
            Toast.makeText(SearchActivity.this, R.string.request_data_null_tips, Toast.LENGTH_SHORT).show();
            return;
        }
        //判断result code
        if(grpUserAddRsp.getRes().getCode() != 200){
            Toast.makeText(SearchActivity.this, grpUserAddRsp.getRes().getMsg(), Toast.LENGTH_SHORT).show();
            return;
        }else{
            Toast.makeText(SearchActivity.this, R.string.search_str_added_friend, Toast.LENGTH_SHORT).show();
            myList.get(myPosition).put("isExist", "1");
            searchGroupListAdapter.notifyDataSetChanged();
        }
    }

    //添加好友请求
    public void userAddFriend(String uidStr, String friendIdStr) {
        loading.show();

        int uid = Integer.parseInt(uidStr);
        int friendId = Integer.parseInt(friendIdStr);
        TalkCloudApp.FriendNewReq friendNewReq = TalkCloudApp.FriendNewReq.newBuilder().setUid(uid).setFuid(friendId).build();
        TalkCloudApp.FriendNewRsp friendNewRsp = null;
        try {
            Future<TalkCloudApp.FriendNewRsp> future = GrpcConnectionManager.getInstance().getGrpcInstantRequestHandler().submit(new Callable<TalkCloudApp.FriendNewRsp>() {
                @Override
                public TalkCloudApp.FriendNewRsp call() throws Exception {
                    return GrpcConnectionManager.getInstance().getBlockingStub().addFriend(friendNewReq);
                }
            });

            friendNewRsp = future.get();
        } catch (Exception e) {
            //TODO Nothing here
        }

        loading.dismiss();

        if(friendNewRsp == null){
            Toast.makeText(SearchActivity.this, R.string.request_data_null_tips, Toast.LENGTH_SHORT).show();
            return;
        }
        //判断friendNewRsp code
        if(friendNewRsp.getRes().getCode() != 200){
            Toast.makeText(SearchActivity.this, friendNewRsp.getRes().getMsg(), Toast.LENGTH_SHORT).show();
            return;
        }else{
            Toast.makeText(SearchActivity.this, R.string.search_str_added_friend, Toast.LENGTH_SHORT).show();
            myList.get(myPosition).put("isFriend", "1");
            searchFriendListAdapter.notifyDataSetChanged();
        }
    }

    //群组列表adapter
    class SearchGroupListAdapter extends BaseAdapter {
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
                convertView = LayoutInflater.from(SearchActivity.this).inflate(R.layout.activity_search_friend_list_item, parent, false);
                holder = new ViewHolder();
                holder.imageView = (ImageView) convertView.findViewById(R.id.search_friend_list_item_image);
                holder.nameTextView = (TextView) convertView.findViewById(R.id.search_friend_list_item_name);
                holder.stateTextView = (TextView) convertView.findViewById(R.id.search_friend_list_item_state);
                holder.textViewBtn = (TextView) convertView.findViewById(R.id.search_friend_list_item_text);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            final Map<String, String> map = myList.get(position);
            holder.nameTextView.setText(map.get("name"));
            holder.stateTextView.setText(map.get("gid"));
            if (map.get("isExist").equals("0")) {
                holder.textViewBtn.setVisibility(View.VISIBLE);
                holder.textViewBtn.setText(R.string.search_str_add_group);
                holder.textViewBtn.setTextColor(getResources().getColor(R.color.color_main_text));
                holder.textViewBtn.setEnabled(true);
            } else {
                holder.textViewBtn.setVisibility(View.VISIBLE);
                holder.textViewBtn.setText(R.string.search_str_added_group);
                holder.textViewBtn.setTextColor(getResources().getColor(R.color.gray_text));
                holder.textViewBtn.setEnabled(false);
            }
            holder.textViewBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    myPosition = position;
                    if (UserBean.getUserBean() != null) {
                        userAddGroup(UserBean.getUserBean().getUserId() + "", map.get("gid"));
                    }
                }
            });

            return convertView;
        }

        class ViewHolder {
            ImageView imageView;
            TextView nameTextView;
            TextView stateTextView;
            TextView textViewBtn;
        }
    }

    //搜索好友Adapter
    class SearchFriendListAdapter extends BaseAdapter {
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
                convertView = LayoutInflater.from(SearchActivity.this).inflate(R.layout.activity_search_friend_list_item, parent, false);
                holder = new ViewHolder();
                holder.imageView = (ImageView) convertView.findViewById(R.id.search_friend_list_item_image);
                holder.nameTextView = (TextView) convertView.findViewById(R.id.search_friend_list_item_name);
                holder.stateTextView = (TextView) convertView.findViewById(R.id.search_friend_list_item_state);
                holder.textViewBtn = (TextView) convertView.findViewById(R.id.search_friend_list_item_text);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            final Map<String, String> map = myList.get(position);
            holder.nameTextView.setText(map.get("name"));
            holder.stateTextView.setText(map.get("uid"));
            if (map.get("isFriend").equals("0")) {
                holder.textViewBtn.setVisibility(View.VISIBLE);
                holder.textViewBtn.setText(R.string.search_str_add_friend);
                holder.textViewBtn.setTextColor(getResources().getColor(R.color.color_main_text));
                holder.textViewBtn.setEnabled(true);
            } else {
                holder.textViewBtn.setVisibility(View.VISIBLE);
                holder.textViewBtn.setText(R.string.search_str_added_friend);
                holder.textViewBtn.setTextColor(getResources().getColor(R.color.gray_text));
                holder.textViewBtn.setEnabled(false);
            }
            holder.textViewBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    myPosition = position;
                    if (UserBean.getUserBean() != null) {
                        userAddFriend(UserBean.getUserBean().getUserId() + "", map.get("uid"));
                    }
                }
            });

            return convertView;
        }

        class ViewHolder {
            ImageView imageView;
            TextView nameTextView;
            TextView stateTextView;
            TextView textViewBtn;
        }
    }
}
