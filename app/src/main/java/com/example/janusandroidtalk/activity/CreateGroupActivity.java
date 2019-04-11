package com.example.janusandroidtalk.activity;

import android.app.Dialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
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

import com.example.janusandroidtalk.R;
import com.example.janusandroidtalk.bean.UserBean;
import com.example.janusandroidtalk.bean.UserFriendBean;
import com.example.janusandroidtalk.dialog.CustomProgressDialog;
import com.example.janusandroidtalk.floatwindow.FloatActionController;
import com.example.janusandroidtalk.signalingcontrol.AudioBridgeControl;
import com.example.janusandroidtalk.signalingcontrol.MyControlCallBack;
import com.example.janusandroidtalk.tools.AppTools;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import talk_cloud.TalkCloudApp;
import talk_cloud.TalkCloudGrpc;

public class CreateGroupActivity extends AppCompatActivity implements MyControlCallBack {

    private EditText editSearch;
    private Dialog loading;
    private TextView menu;

    private List<UserFriendBean> myList = new ArrayList<>();
    private ImageView toolbarBack = null;
    private ListView listView;

    private GroupListAdapter groupListAdapter;

    private int sum = 0;
    private String userIds = "";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);
        editSearch = (EditText) findViewById(R.id.create_group_edit);
        toolbarBack = (ImageView) findViewById(R.id.create_group_back);
        listView = (ListView) findViewById(R.id.create_group_list_view);
        menu = (TextView) findViewById(R.id.create_group_menu);

        //赋值用户好友列表
        if (UserBean.getUserBean() != null) {
            myList = UserBean.getUserBean().getUserFriendBeanArrayList();
        }

        loading = CustomProgressDialog.createLoadingDialog(this, R.string.recycler_pull_loading);
        loading.setCancelable(true);
        loading.setCanceledOnTouchOutside(false);

        //隐藏悬浮窗
        FloatActionController.getInstance().hide();

        //返回事件
        toolbarBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (UserFriendBean userFriendBean: myList) {
                    userFriendBean.setCheck(false);
                }
                CreateGroupActivity.this.finish();
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
                //创建群组
                for (UserFriendBean userFriendBean: myList) {
                    if(userFriendBean.isCheck()){
                        userIds = userIds +","+userFriendBean.getUserFriendId();
                    }
                }
                new GrpcCreateGroupTask().execute(userIds);
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
                convertView = LayoutInflater.from(CreateGroupActivity.this).inflate(R.layout.activity_create_group_list_item, parent, false);
                holder = new ViewHolder();
                holder.imageView = (ImageView) convertView.findViewById(R.id.create_group_list_item_image);
                holder.nameTextView = (TextView) convertView.findViewById(R.id.create_group_list_item_name);
                holder.stateTextView = (TextView) convertView.findViewById(R.id.create_group_list_item_state);
                holder.checkBox = (CheckBox) convertView.findViewById(R.id.create_group_list_item_checkbox);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.nameTextView.setText(myList.get(position).getUserFriendName());
            holder.stateTextView.setText(myList.get(position).getUserFriendId() + "");
            holder.checkBox.setOnCheckedChangeListener(null);
            holder.checkBox.setChecked(myList.get(position).isCheck());
            holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        myList.get(position).setCheck(true);
                        sum = sum + 1;
                        menu.setText("确定(" + sum + ")");
                    } else {
                        sum = sum - 1;
                        if (sum == 0) {
                            menu.setText(R.string.toolbar_commit);
                        } else {
                            menu.setText("确定(" + sum + ")");
                        }
                        myList.get(position).setCheck(false);
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
                        mOriginalValues = new ArrayList<UserFriendBean>(myList);
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
                myList = (List<UserFriendBean>) results.values;
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
        }
    }

    //添加群组请求
    class GrpcCreateGroupTask extends AsyncTask<String, Void, TalkCloudApp.CreateGroupResp> {
        private ManagedChannel channel;

        private GrpcCreateGroupTask() {
            loading.show();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected TalkCloudApp.CreateGroupResp doInBackground(String... params) {
            String groupName = "";
            int accountId = 0;
            if(UserBean.getUserBean() != null){
                groupName = UserBean.getUserBean().getUserName()+"的群组"+ (int)(Math.random()*100);
                accountId = UserBean.getUserBean().getUserId();
            }

            TalkCloudApp.CreateGroupResp replay = null;
            try {
                channel = ManagedChannelBuilder.forAddress(AppTools.host, AppTools.port).usePlaintext().build();
                TalkCloudGrpc.TalkCloudBlockingStub stub = TalkCloudGrpc.newBlockingStub(channel);
                TalkCloudApp.CreateGroupReq regReq = TalkCloudApp.CreateGroupReq.newBuilder().setDeviceIds(params[0]).setGroupName(groupName).setAccountId(accountId).build();
                replay = stub.createGroup(regReq);
                return replay;
            } catch (Exception e) {
                return replay;
            }
        }

        @Override
        protected void onPostExecute(TalkCloudApp.CreateGroupResp result) {
            try {
                channel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            loading.dismiss();

            if (result == null) {
                Toast.makeText(CreateGroupActivity.this, R.string.request_data_null_tips, Toast.LENGTH_SHORT).show();
                return;
            }
            //判断result code
            if (result.getRes().getCode() != 200) {
                Toast.makeText(CreateGroupActivity.this, result.getRes().getMsg(), Toast.LENGTH_SHORT).show();
                return;
            } else {
                //创建成功
                AudioBridgeControl.sendCreateGroup(CreateGroupActivity.this,(int)(result.getGroupInfo().getGid()));
            }

        }
    }

    //回调信息处理，
    @Override
    public void showMessage(final JSONObject msg) {
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

    private Handler handler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case 1:
                    Toast.makeText(CreateGroupActivity.this, R.string.new_group_str_create_success, Toast.LENGTH_SHORT).show();
                    for (UserFriendBean userFriendBean: myList) {
                        userFriendBean.setCheck(false);
                    }
                    CreateGroupActivity.this.finish();
                    break;
            }
        };
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            for (UserFriendBean userFriendBean: myList) {
                userFriendBean.setCheck(false);
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
