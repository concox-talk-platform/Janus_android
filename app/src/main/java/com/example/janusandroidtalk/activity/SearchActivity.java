package com.example.janusandroidtalk.activity;

import android.app.Dialog;
import android.os.AsyncTask;
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
import com.example.janusandroidtalk.tools.AppTools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import talk_cloud.TalkCloudApp;
import talk_cloud.TalkCloudGrpc;

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
                                    new GrpcSearchGroupListTask().execute(UserBean.getUserBean().getUserId() + "", editSearch.getText().toString());
                                }
                                break;
                            case 1:
                                //搜索好友
                                if (UserBean.getUserBean() != null) {
                                    new GrpcSearchFriendListTask().execute(UserBean.getUserBean().getUserId() + "", editSearch.getText().toString());
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
    class GrpcSearchGroupListTask extends AsyncTask<String, Void, TalkCloudApp.GroupListRsp> {
        private ManagedChannel channel;

        private GrpcSearchGroupListTask() {

        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loading.show();
        }

        @Override
        protected TalkCloudApp.GroupListRsp doInBackground(String... params) {
            int id = Integer.parseInt(params[0]);
            TalkCloudApp.GroupListRsp replay = null;
            try {
                channel = ManagedChannelBuilder.forAddress(AppTools.host, AppTools.port).usePlaintext().build();
                TalkCloudGrpc.TalkCloudBlockingStub stub = TalkCloudGrpc.newBlockingStub(channel);
                TalkCloudApp.GrpSearchReq regReq = TalkCloudApp.GrpSearchReq.newBuilder().setUid(id).setTarget(params[1]).build();
                replay = stub.searchGroup(regReq);
                return replay;
            } catch (Exception e) {
                return replay;
            }
        }

        @Override
        protected void onPostExecute(TalkCloudApp.GroupListRsp result) {
            try {
                channel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            loading.dismiss();
            if(result == null){
                Toast.makeText(SearchActivity.this,R.string.request_data_null_tips,Toast.LENGTH_SHORT).show();
                return;
            }
            //判断result code
            if(result.getRes().getCode() != 200){
                Toast.makeText(SearchActivity.this,result.getRes().getMsg(),Toast.LENGTH_SHORT).show();
                return;
            }
            //判断列表数据为空
            if(result.getGroupListList().size() == 0){
                Toast.makeText(SearchActivity.this,R.string.request_data_null_tips,Toast.LENGTH_SHORT).show();
                return;
            }

            myList.clear();
            for (TalkCloudApp.GroupInfo groupListRsp : result.getGroupListList()) {
                Map<String,String> map1 = new HashMap<String,String>();
                map1.put("name",groupListRsp.getGroupName());
                map1.put("gid",groupListRsp.getGid()+"");
                if(groupListRsp.getIsExist()){
                    map1.put("isExist","1");
                }else{
                    map1.put("isExist","0");
                }
                myList.add(map1);
            }

            searchGroupListAdapter = new SearchGroupListAdapter();
            listView.setAdapter(searchGroupListAdapter);
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
                        new GrpcAddGroupTask().execute(UserBean.getUserBean().getUserId() + "", map.get("gid"));
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

    //添加群组请求
    class GrpcAddGroupTask extends AsyncTask<String, Void,TalkCloudApp.GrpUserAddRsp> {
        private ManagedChannel channel;

        private GrpcAddGroupTask() {
            loading.show();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected TalkCloudApp.GrpUserAddRsp doInBackground(String... params) {
            int id = Integer.parseInt(params[0]);
            int gid = Integer.parseInt(params[1]);
            TalkCloudApp.GrpUserAddRsp replay = null;
            try {
                channel = ManagedChannelBuilder.forAddress(AppTools.host, AppTools.port).usePlaintext().build();
                TalkCloudGrpc.TalkCloudBlockingStub stub = TalkCloudGrpc.newBlockingStub(channel);
                TalkCloudApp.GrpUserAddReq regReq = TalkCloudApp.GrpUserAddReq.newBuilder().setUid(id).setGid(gid).build();
                replay = stub.joinGroup(regReq);
                return replay;
            } catch (Exception e) {
                return replay;
            }
        }

        @Override
        protected void onPostExecute(TalkCloudApp.GrpUserAddRsp result) {
            try {
                channel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            loading.dismiss();

            if(result == null){
                Toast.makeText(SearchActivity.this,R.string.request_data_null_tips,Toast.LENGTH_SHORT).show();
                return;
            }
            //判断result code
            if(result.getRes().getCode() != 200){
                Toast.makeText(SearchActivity.this,result.getRes().getMsg(),Toast.LENGTH_SHORT).show();
                return;
            }else{
                Toast.makeText(SearchActivity.this,R.string.search_str_added_friend,Toast.LENGTH_SHORT).show();
                myList.get(myPosition).put("isExist","1");
                searchGroupListAdapter.notifyDataSetChanged();
            }

        }
    }

    //搜索好友列表请求
    class GrpcSearchFriendListTask extends AsyncTask<String, Void,TalkCloudApp.UserSearchRsp> {
        private ManagedChannel channel;

        private GrpcSearchFriendListTask() {

        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loading.show();
        }

        @Override
        protected TalkCloudApp.UserSearchRsp doInBackground(String... params) {
            int id = Integer.parseInt(params[0]);
            TalkCloudApp.UserSearchRsp replay = null;
            try {
                channel = ManagedChannelBuilder.forAddress(AppTools.host, AppTools.port).usePlaintext().build();
                TalkCloudGrpc.TalkCloudBlockingStub stub = TalkCloudGrpc.newBlockingStub(channel);
                TalkCloudApp.UserSearchReq regReq = TalkCloudApp.UserSearchReq.newBuilder().setUid(id).setTarget(params[1]).build();
                replay = stub.searchUserByKey(regReq);
                return replay;
            } catch (Exception e) {
                return replay;
            }
        }

        @Override
        protected void onPostExecute(TalkCloudApp.UserSearchRsp result) {
            try {
                channel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            loading.dismiss();
            if(result == null){
                Toast.makeText(SearchActivity.this,R.string.request_data_null_tips,Toast.LENGTH_SHORT).show();
                return;
            }
            //判断result code
            if(result.getRes().getCode() != 200){
                Toast.makeText(SearchActivity.this,result.getRes().getMsg(),Toast.LENGTH_SHORT).show();
                return;
            }

            //判断列表数据为空
            if(result.getUserListList().size() == 0){
                Toast.makeText(SearchActivity.this,R.string.request_data_null_tips,Toast.LENGTH_SHORT).show();
                return;
            }

            myList.clear();
            for (TalkCloudApp.UserRecord userRecord : result.getUserListList()) {
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
                        new GrpcAddFriendTask().execute(UserBean.getUserBean().getUserId() + "", map.get("uid"));
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

    //添加好友请求
    class GrpcAddFriendTask extends AsyncTask<String, Void,TalkCloudApp.FriendNewRsp> {
        private ManagedChannel channel;

        private GrpcAddFriendTask() {
            loading.show();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected TalkCloudApp.FriendNewRsp doInBackground(String... params) {
            int id = Integer.parseInt(params[0]);
            int fid = Integer.parseInt(params[1]);
            TalkCloudApp.FriendNewRsp replay = null;
            try {
                channel = ManagedChannelBuilder.forAddress(AppTools.host, AppTools.port).usePlaintext().build();
                TalkCloudGrpc.TalkCloudBlockingStub stub = TalkCloudGrpc.newBlockingStub(channel);
                TalkCloudApp.FriendNewReq regReq = TalkCloudApp.FriendNewReq.newBuilder().setUid(id).setFuid(fid).build();
                replay = stub.addFriend(regReq);
                return replay;
            } catch (Exception e) {
                return replay;
            }
        }

        @Override
        protected void onPostExecute(TalkCloudApp.FriendNewRsp result) {
            try {
                channel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            loading.dismiss();

            if(result == null){
                Toast.makeText(SearchActivity.this,R.string.request_data_null_tips,Toast.LENGTH_SHORT).show();
                return;
            }
            //判断result code
            if(result.getRes().getCode() != 200){
                Toast.makeText(SearchActivity.this,result.getRes().getMsg(),Toast.LENGTH_SHORT).show();
                return;
            }else{
                Toast.makeText(SearchActivity.this,R.string.search_str_added_friend,Toast.LENGTH_SHORT).show();
                myList.get(myPosition).put("isFriend","1");
                searchFriendListAdapter.notifyDataSetChanged();
            }

        }
    }
}
