package com.example.janusandroidtalk.im.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.janusandroidtalk.R;
import com.example.janusandroidtalk.bean.UserBean;
import com.example.janusandroidtalk.floatwindow.FloatActionController;
import com.example.janusandroidtalk.im.SingleControll;
import com.example.janusandroidtalk.im.model.ChatMessage;
import com.example.janusandroidtalk.im.model.DefaultUser;
import com.example.janusandroidtalk.im.record.ChatDBManager;
import com.example.janusandroidtalk.im.utils.PictureFileUtil;
import com.example.janusandroidtalk.im.view.ChatUiHelper;
import com.example.janusandroidtalk.im.view.ChatView;
import com.example.janusandroidtalk.im.view.RecordButton;
import com.example.janusandroidtalk.im.view.StateButton;
import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.entity.LocalMedia;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import cn.jiguang.imui.commons.ImageLoader;
import cn.jiguang.imui.commons.models.IMessage;
import cn.jiguang.imui.messages.MsgListAdapter;
import cn.jiguang.imui.messages.ptr.PtrHandler;
import cn.jiguang.imui.messages.ptr.PullToRefreshLayout;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class SingleChatActivity extends Activity implements View.OnClickListener, View.OnTouchListener, EasyPermissions.PermissionCallbacks{
    private int mUserId;
    private String mUserName;
    private ChatView mChatView;
    private List<ChatMessage> mData;
    private MsgListAdapter<ChatMessage> mAdapter;
    private InputMethodManager mImm;
    private Window mWindow;

    private LinearLayout mLlContent;
    private RecyclerView mRvChat;
    private EditText mEtContent;
    private RelativeLayout mRlBottomLayout;//表情,添加底部布局
    private ImageView mIvAdd;
    private StateButton mBtnSend;//发送按钮
    private ImageView mIvAudio;//录音图片
    private RecordButton mBtnAudio;//录音按钮
    private LinearLayout mLlAdd;//添加布局
    private RelativeLayout mRlPhoto;
    private RelativeLayout mRlVedio;
    private RelativeLayout mRlFile;
    private UpdateDataBroadcastReceiver updateReceiver;

    private static final int REQUEST_CODE_IMAGE = 1001;
    private static final int REQUEST_CODE_VEDIO = 1002;

    //存储所有图像消息的路径，将其传递给BrowserImageActivity以便单击图像消息可以浏览所有图像
    private ArrayList<String> mPathList = new ArrayList<>();
    private ArrayList<String> mMsgIdList = new ArrayList<>();

    private ChatDBManager mDbManager;
    private SingleControll mSingleControll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_groupchat);

        //隐藏悬浮窗
        FloatActionController.getInstance().hide();

        this.mImm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mWindow = getWindow();
        Intent intent = getIntent();
        mUserId = intent.getIntExtra("userid", 0);
        mUserName = intent.getStringExtra("username");
        Log.d("SingleChatActivity", "This is onCreate userid=" + mUserId + " username=" + mUserName);
        initView();
        initChatUi();
        initData();
        initMsgAdapter();
    }

    private void initView() {
        mChatView = (ChatView) findViewById(R.id.chat_view);
        mLlContent = findViewById(R.id.llContent);
        mRvChat = findViewById(R.id.msg_list);
        mEtContent = findViewById(R.id.et_content);
        mRlBottomLayout = findViewById(R.id.bottom_layout);
        mIvAdd = findViewById(R.id.ivAdd);
        mBtnSend = findViewById(R.id.btn_send);
        mIvAudio = findViewById(R.id.ivAudio);
        mBtnAudio = findViewById(R.id.btnAudio);
        mLlAdd = findViewById(R.id.llAdd);
        mRlPhoto = findViewById(R.id.rlPhoto);
        mRlVedio = findViewById(R.id.rlVideo);
        mRlFile = findViewById(R.id.rlFile);

        mChatView.initModule();
        mChatView.setTitle(mUserId + "");

        mBtnSend.setOnClickListener(this);
        mRlPhoto.setOnClickListener(this);
        mRlVedio.setOnClickListener(this);
        mRlFile.setOnClickListener(this);
        mSingleControll = new SingleControll();
        mSingleControll.setOnSingleInfoListener(new SingleControll.OnSingleInfoListener() {
            @Override
            public void OnTextMsg(boolean b,String msgId) {
                Log.d("SingleChatActivity"," this is OnGroupInfo OnTextMsg = "+b+" msgId="+msgId);
                modifyMsgStatus(b,msgId);//更新文字的发送状态
            }

            @Override
            public void OnMediaMsg(boolean b, String msgId) {
                Log.d("SingleChatActivity"," this is OnGroupInfo OnMediaMsg = "+b+" msgId="+msgId);
                modifyMsgStatus(b,msgId);//更新图片、语音、视频的发送状态
            }
        });
    }

    /**
     * 更新message的发送状态
     * @param b      false发送失败,true发送成功
     * @param msgId  要更新状态的message id
     */
    private void modifyMsgStatus(boolean b,String msgId){
        ChatMessage message = mDbManager.getRecordForMsgId(msgId);
        if(message.getType()==IMessage.MessageType.RECEIVE_IMAGE.ordinal()||
                message.getType()==IMessage.MessageType.SEND_IMAGE.ordinal()){
            for(int i=0;i<mMsgIdList.size();i++){
                if(mMsgIdList.get(i).equals(msgId)){
                    mMsgIdList.set(i,message.getMsgId());
                }
            }
        }
        if(b){
            message.setMessageStatus(IMessage.MessageStatus.SEND_SUCCEED);
            mAdapter.updateMessage(msgId,message);
            mDbManager.modifyRecordMsg(msgId,IMessage.MessageStatus.SEND_SUCCEED.ordinal());
        }else {
            message.setMessageStatus(IMessage.MessageStatus.SEND_FAILED);
            mAdapter.updateMessage(msgId,message);
            mDbManager.modifyRecordMsg(msgId,IMessage.MessageStatus.SEND_FAILED.ordinal());
        }
    }

    private void initChatUi() {
        //mBtnAudio
        final ChatUiHelper mUiHelper = ChatUiHelper.with(this);
        mUiHelper.bindContentLayout(mLlContent)
                .bindttToSendButton(mBtnSend)
                .bindEditText(mEtContent)
                .bindBottomLayout(mRlBottomLayout)
                .bindAddLayout(mLlAdd)
                .bindToAddButton(mIvAdd)
                .bindAudioBtn(mBtnAudio)
                .bindAudioIv(mIvAudio);
        //底部布局弹出,聊天列表上滑
        mRvChat.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (bottom < oldBottom) {
                    mRvChat.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mAdapter.getItemCount() > 0) {
                                scrollToBottom();
                            }
                        }
                    });
                }
            }
        });
        //点击空白区域关闭键盘
        mRvChat.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                mUiHelper.hideBottomLayout(false);
                mUiHelper.hideSoftInput();
                mEtContent.clearFocus();
                return false;
            }
        });
        ((RecordButton) mBtnAudio).setOnFinishedRecordListener(new RecordButton.OnFinishedRecordListener() {
            @Override
            public void onFinishedRecord(String audioPath, int time) {
                //录音结束回调
                File file = new File(audioPath);
                if (file.exists()) {
                    sendAudioMessage(audioPath, time);
                }
            }
        });
    }

    private void initData() {
        mDbManager = new ChatDBManager(this);
        mData = getMessages();
        Log.d("SingleChatActivity", " this is InitData() mData size=" + mData.size());
        IntentFilter filter = new IntentFilter();
        filter.addAction("com_jimi_chat_updatedata");
        updateReceiver = new UpdateDataBroadcastReceiver();
        registerReceiver(updateReceiver,filter);
    }

    /**
     * 获取该群组的历史消息
     * @return
     */
    private List<ChatMessage> getMessages() {
        List<ChatMessage> list = mDbManager.getRecord(UserBean.getUserBean().getUserId(), mUserId);
        Log.d("SingleChatActivity", " this is getMessage() chatrecord list=" + list.size());
        for (int i=0;i<list.size();i++){
            if(list.get(i).getType()==IMessage.MessageType.RECEIVE_IMAGE.ordinal()||list.get(i).getType()==IMessage.MessageType.SEND_IMAGE.ordinal()){
                mPathList.add(list.get(i).getMediaFilePath());
                mMsgIdList.add(list.get(i).getMsgId());
            }
        }

        List<ChatMessage> list1 = mDbManager.getRecord(mUserId, UserBean.getUserBean().getUserId());
        Log.d("SingleChatActivity", " this is getMessage() chatrecord list1=" + list1.size());
        for (int i = 0; i < list1.size(); i++) {
            if (list1.get(i).getType() == IMessage.MessageType.RECEIVE_IMAGE.ordinal() || list1.get(i).getType() == IMessage.MessageType.SEND_IMAGE.ordinal()) {
                mPathList.add(list1.get(i).getMediaFilePath());
                mMsgIdList.add(list1.get(i).getMsgId());
            }
        }

        for (int i = 0; i < list.size(); i++) {
            Log.d("SingleChatActivity", " this is list msg = " + list.get(i).getText() + " record timestamp = " + list.get(i).getTimestamp());
        }
        for (int i = 0; i < list1.size(); i++) {
            Log.d("SingleChatActivity", " this is list1 msg = " + list1.get(i).getText() + " record timestamp = " + list1.get(i).getTimestamp());
        }

        ArrayList<ChatMessage> chatMessagesHistory = new ArrayList<>();
        int i = 0, j = 0;
        while (true) {
            if (i == list.size()) {
                for (int k = j; k < list1.size(); k++) {
                    chatMessagesHistory.add(list1.get(k));
                }
                break;
            }

            if (j == list1.size()) {
                for (int k = i; k < list.size(); k++) {
                    chatMessagesHistory.add(list.get(k));
                }
                break;
            }

            if (list.get(i).getTimestamp() < list1.get(j).getTimestamp())
            {
                chatMessagesHistory.add(list.get(i));
                i++;
            }
            else {
                chatMessagesHistory.add(list1.get(j));
                j++;
            }
        }
        Log.d("SingleChatActivity", " this is getMessage() chatrecord chatMessagesHistory=" + chatMessagesHistory.size());

        for (int k = 0; k < chatMessagesHistory.size(); k++) {
            Log.d("SingleChatActivity", " this is getMessage() chatrecord chatMessagesHistory msg = " + chatMessagesHistory.get(k).getText() + " record timestamp = " + chatMessagesHistory.get(k).getTimestamp());
        }

        return chatMessagesHistory;
    }

    private void initMsgAdapter() {
        final float density = getResources().getDisplayMetrics().density;
        final float MIN_WIDTH = 60 * density;
        final float MAX_WIDTH = 200 * density;
        final float MIN_HEIGHT = 60 * density;
        final float MAX_HEIGHT = 200 * density;
        ImageLoader imageLoader = new ImageLoader() {
            @Override
            public void loadAvatarImage(ImageView avatarImageView, String string) {
                //加载头像图片
                if (string.contains("R.drawable")) {
                    Integer resId = getResources().getIdentifier(string.replace("R.drawable.", ""),
                            "drawable", getPackageName());

                    avatarImageView.setImageResource(resId);
                } else {
                    //默认头像
                    Glide.with(SingleChatActivity.this)
                            .load(string)
                            .apply(new RequestOptions().placeholder(R.drawable.aurora_headicon_default))
                            .into(avatarImageView);
                }
            }

            /**
             * 加载图片
             * @param imageView 要加载图片的控件
             * @param string 文件路径
             */
            @Override
            public void loadImage(final ImageView imageView, String string) {
                Glide.with(getApplicationContext())
                        .asBitmap()
                        .load(string)
                        .apply(new RequestOptions().fitCenter().placeholder(R.drawable.aurora_picture_not_found))
                        .into(new SimpleTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                int imageWidth = resource.getWidth();
                                int imageHeight = resource.getHeight();
                                Log.d("SingleChatActivity", "Image width " + imageWidth + " height: " + imageHeight);

                                // 裁剪 bitmap
                                float width, height;
                                if (imageWidth > imageHeight) {
                                    if (imageWidth > MAX_WIDTH) {
                                        float temp = MAX_WIDTH / imageWidth * imageHeight;
                                        height = temp > MIN_HEIGHT ? temp : MIN_HEIGHT;
                                        width = MAX_WIDTH;
                                    } else if (imageWidth < MIN_WIDTH) {
                                        float temp = MIN_WIDTH / imageWidth * imageHeight;
                                        height = temp < MAX_HEIGHT ? temp : MAX_HEIGHT;
                                        width = MIN_WIDTH;
                                    } else {
                                        float ratio = imageWidth / imageHeight;
                                        if (ratio > 3) {
                                            ratio = 3;
                                        }
                                        height = imageHeight * ratio;
                                        width = imageWidth;
                                    }
                                } else {
                                    if (imageHeight > MAX_HEIGHT) {
                                        float temp = MAX_HEIGHT / imageHeight * imageWidth;
                                        width = temp > MIN_WIDTH ? temp : MIN_WIDTH;
                                        height = MAX_HEIGHT;
                                    } else if (imageHeight < MIN_HEIGHT) {
                                        float temp = MIN_HEIGHT / imageHeight * imageWidth;
                                        width = temp < MAX_WIDTH ? temp : MAX_WIDTH;
                                        height = MIN_HEIGHT;
                                    } else {
                                        float ratio = imageHeight / imageWidth;
                                        if (ratio > 3) {
                                            ratio = 3;
                                        }
                                        width = imageWidth * ratio;
                                        height = imageHeight;
                                    }
                                }
                                ViewGroup.LayoutParams params = imageView.getLayoutParams();
                                params.width = (int) width;
                                params.height = (int) height;
                                imageView.setLayoutParams(params);
                                Matrix matrix = new Matrix();
                                float scaleWidth = width / imageWidth;
                                float scaleHeight = height / imageHeight;
                                matrix.postScale(scaleWidth, scaleHeight);
                                imageView.setImageBitmap(Bitmap.createBitmap(resource, 0, 0, imageWidth, imageHeight, matrix, true));
                            }
                        });
            }

            /**
             * 加载视频
             * @param imageCover 视频消息的图像封面
             * @param uri 视频路径
             */
            @Override
            public void loadVideo(ImageView imageCover, String uri) {
                //Toast.makeText(GroupChatActivity.this, "this is loadVideo", Toast.LENGTH_SHORT).show();
                long interval = 5000 * 1000;
                Glide.with(SingleChatActivity.this)
                        .asBitmap()
                        .load(uri)
                        // Resize image view by change override size.
                        .apply(new RequestOptions().frame(interval).override(100, 150))//视频图像显示的大小with:100,height:150
                        .into(imageCover);
            }
        };

        MsgListAdapter.HoldersConfig holdersConfig = new MsgListAdapter.HoldersConfig();
        mAdapter = new MsgListAdapter<>("0", holdersConfig, imageLoader);

        mAdapter.setOnMsgClickListener(new MsgListAdapter.OnMsgClickListener<ChatMessage>() {
            @Override
            public void onMessageClick(ChatMessage message) {
                // 点击消息事件
                if (message.getType() == IMessage.MessageType.RECEIVE_VIDEO.ordinal()
                        || message.getType() == IMessage.MessageType.SEND_VIDEO.ordinal()) {//视频播放
                    if (!TextUtils.isEmpty(message.getMediaFilePath())) {
                        Intent intent = new Intent(SingleChatActivity.this, VideoActivity.class);
                        intent.putExtra(VideoActivity.VIDEO_PATH, message.getMediaFilePath());
                        startActivity(intent);
                    }
                } else if (message.getType() == IMessage.MessageType.RECEIVE_IMAGE.ordinal()
                        || message.getType() == IMessage.MessageType.SEND_IMAGE.ordinal()) {//图片显示
                    Intent intent = new Intent(SingleChatActivity.this, BrowserImageActivity.class);
                    intent.putExtra("msgId", message.getMsgId());
                    intent.putStringArrayListExtra("pathList", mPathList);
                    intent.putStringArrayListExtra("idList", mMsgIdList);
                    startActivity(intent);
                } else {
                    /*Toast.makeText(getApplicationContext(), "点击消息事件",
                            Toast.LENGTH_SHORT).show();*/
                }
            }
        });

        mAdapter.setMsgLongClickListener(new MsgListAdapter.OnMsgLongClickListener<ChatMessage>() {
            @Override
            public void onMessageLongClick(View view, ChatMessage message) {
                //长按消息事件的处理
            }
        });

        mAdapter.setOnAvatarClickListener(new MsgListAdapter.OnAvatarClickListener<ChatMessage>() {
            @Override
            public void onAvatarClick(ChatMessage message) {
                DefaultUser userInfo = (DefaultUser) message.getFromUser();
                //头像点击事件
            }
        });

        mAdapter.setMsgStatusViewClickListener(new MsgListAdapter.OnMsgStatusViewClickListener<ChatMessage>() {
            @Override
            public void onStatusViewClick(ChatMessage message) {
                // 这里是消息状态的点击事件,可以在此处做重新发送处理
            }
        });

        if (mData != null && mData.size() > 0)
            mAdapter.addToEndChronologically(mData);
        PullToRefreshLayout layout = mChatView.getPtrLayout();
        layout.setPtrHandler(new PtrHandler() {
            @Override
            public void onRefreshBegin(PullToRefreshLayout layout) {
                Log.i("MessageListActivity", "Loading next page");
                loadNextPage();
            }
        });
        // Deprecated, should use onRefreshBegin to load next page
        mAdapter.setOnLoadMoreListener(new MsgListAdapter.OnLoadMoreListener() {
            @Override
            public void onLoadMore(int page, int totalCount) {
//                Log.i("MessageListActivity", "Loading next page");
//                loadNextPage();
            }
        });

        mChatView.setAdapter(mAdapter);
        mAdapter.getLayoutManager().scrollToPosition(0);//滑动到底部
    }

    //下一页,暂时没做分页处理
    private void loadNextPage() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                List<ChatMessage> list = new ArrayList<>();
                mAdapter.addToEndChronologically(list);
                mChatView.getPtrLayout().refreshComplete();
            }
        }, 1500);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                try {
                    //触摸事件,隐藏键盘
                    View v = getCurrentFocus();
                    if (mImm != null && v != null) {
                        mImm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                        mWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                        view.clearFocus();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case MotionEvent.ACTION_UP:
                view.performClick();
                break;
        }
        return false;
    }

    /**
     * 滑动到底部
     */
    private void scrollToBottom() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mChatView.getMessageListView().smoothScrollToPosition(0);
            }
        }, 200);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_send:
                //发送文字,同时清空编辑框
                sendTextMsg(mEtContent.getText().toString());
                mEtContent.setText("");
                break;
            case R.id.rlPhoto:
                PictureFileUtil.openGalleryPic(SingleChatActivity.this, REQUEST_CODE_IMAGE);//图片选择器
                break;
            case R.id.rlVideo:
                PictureFileUtil.openGalleryAudio(SingleChatActivity.this, REQUEST_CODE_VEDIO);//视频选择器
                break;
            case R.id.rlFile:
                //这里是文件发送按钮
                //PictureFileUtil.openFile(ChatActivity.this,REQUEST_CODE_FILE);
                break;
            case R.id.rlLocation:
                //发送位置
                break;
        }
    }

    /**
     *  发送文字
     * @param text
     */
    private void sendTextMsg(String text) {
        if (UserBean.getUserBean().isOnline() == 2) {
            ChatMessage textMessage = new ChatMessage();
            textMessage.setText(text);
            textMessage.setType(IMessage.MessageType.SEND_TEXT.ordinal());
            textMessage.setUserInfo(new DefaultUser(UserBean.getUserBean().getUserId() + "", UserBean.getUserBean().getUserName(), ""));
            textMessage.setTimeString(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
            textMessage.setReceiveId(mUserId);
            textMessage.setTimestamp(System.currentTimeMillis());
            mAdapter.addToStart(textMessage, true);
            mDbManager.addRecord(textMessage);
            Log.d("SingleChatActivity", " this is sendTextMsg() data=" + textMessage.toString());
            new SingleControll.ChatTextTask().execute(textMessage);
        }
        else {
            Toast.makeText(this, "发送失败，你离线了！", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 发送语音
     * @param path
     * @param time
     */
    private void sendAudioMessage(String path, int time) {
        ChatMessage voiceMessage = new ChatMessage();
        voiceMessage.setType(IMessage.MessageType.SEND_VOICE.ordinal());
        voiceMessage.setUserInfo(new DefaultUser(UserBean.getUserBean().getUserId() + "", UserBean.getUserBean().getUserName(), ""));
        voiceMessage.setTimeString(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
        voiceMessage.setMediaFilePath(path);
        voiceMessage.setDuration(time);
        voiceMessage.setReceiveId(mUserId);
        voiceMessage.setReceiveName(mUserName);
        voiceMessage.setTimestamp(System.currentTimeMillis());
        mAdapter.addToStart(voiceMessage, true);
        mDbManager.addRecord(voiceMessage);
        //Toast.makeText(this, "发送语音", Toast.LENGTH_SHORT).show();
        new SingleControll.ChatTask().execute(voiceMessage);
    }

    /**
     * 发送图片
     * @param path
     */
    private void sendImageMessage(String path) {
        ChatMessage imageMessage = new ChatMessage();
        imageMessage.setType(IMessage.MessageType.SEND_IMAGE.ordinal());
        imageMessage.setUserInfo(new DefaultUser(UserBean.getUserBean().getUserId() + "", UserBean.getUserBean().getUserName(), ""));
        imageMessage.setTimeString(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
        imageMessage.setMediaFilePath(path);
        mPathList.add(path);
        mMsgIdList.add(imageMessage.getMsgId());
        imageMessage.setReceiveId(mUserId);
        imageMessage.setReceiveName(mUserName);
        imageMessage.setTimestamp(System.currentTimeMillis());
        mAdapter.addToStart(imageMessage, true);
        mDbManager.addRecord(imageMessage);
        //Toast.makeText(this, "发送图片 msgid="+imageMessage.getMsgId(), Toast.LENGTH_SHORT).show();
        new SingleControll.ChatTask().execute(imageMessage);
    }

    /**
     * 发送视频
     * @param path
     * @param duration
     */
    private void sendVedioMessage(String path, long duration) {
        //Toast.makeText(this, "video duration=" + duration, Toast.LENGTH_SHORT).show();
        ChatMessage vedioMessage = new ChatMessage();
        vedioMessage.setType(IMessage.MessageType.SEND_VIDEO.ordinal());
        vedioMessage.setUserInfo(new DefaultUser(UserBean.getUserBean().getUserId() + "", UserBean.getUserBean().getUserName(), ""));
        vedioMessage.setTimeString(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
        vedioMessage.setDuration(duration);
        vedioMessage.setMediaFilePath(path);
        vedioMessage.setReceiveId(mUserId);
        vedioMessage.setReceiveName(mUserName);
        vedioMessage.setTimestamp(System.currentTimeMillis());
        mAdapter.addToStart(vedioMessage, true);
        mDbManager.addRecord(vedioMessage);
        //Toast.makeText(this, "发送视频", Toast.LENGTH_SHORT).show();
        new SingleControll.ChatTask().execute(vedioMessage);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                /*case REQUEST_CODE_FILE:
                    String filePath = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);
                    sendFileMessage(mSenderId, mTargetId, filePath);
                    break;*/
                case REQUEST_CODE_IMAGE:
                    // 图片选择结果回调
                    List<LocalMedia> selectListPic = PictureSelector.obtainMultipleResult(data);
                    for (LocalMedia media : selectListPic) {
                        sendImageMessage(media.getPath());
                    }
                    break;
                case REQUEST_CODE_VEDIO:
                    // 视频选择结果回调
                    List<LocalMedia> selectListVideo = PictureSelector.obtainMultipleResult(data);
                    for (LocalMedia media : selectListVideo) {
                        Log.d("SingleChatActivity", " duration=" + media.getDuration());
                        sendVedioMessage(media.getPath(), media.getDuration() / 1000);
                    }
                    break;
            }
        }
    }

    /**
     * 更新服务器下发消息的广播
     */
    public class UpdateDataBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if("com_jimi_chat_updatedata".equals(intent.getAction())){
                long id = intent.getLongExtra("id",-1);
                Log.d("SingleChatActivity", "this is UpdateDataBroadcastReceiver onReceive this is UpdateDataBroadcastReceiver id="+id);
                if(id != -1){
                    ChatMessage msg = mDbManager.getRecordForId(id);
                    Log.d("SingleChatActivity", "this is UpdateDataBroadcastReceiver onReceive msg from id="+msg.getFromUser().getId());

                    Log.d("SingleChatActivity", "this is UpdateDataBroadcastReceiver onReceive msg.getReceiveId() = " + msg.getReceiveId() + " mUserId = " + mUserId);
                    Log.d("SingleChatActivity", "this is UpdateDataBroadcastReceiver onReceive msg.getReceiveId() = " + msg.getReceiveId() + " my id = " + UserBean.getUserBean().getUserId());
                    if(msg != null && msg.getReceiveId() == UserBean.getUserBean().getUserId()){
                        if(msg.getType()==IMessage.MessageType.RECEIVE_IMAGE.ordinal()||
                                msg.getType()==IMessage.MessageType.SEND_IMAGE.ordinal()){
                            mPathList.add(msg.getMediaFilePath());
                            mMsgIdList.add(msg.getMsgId());
                        }
                        mAdapter.addToStart(msg,true);
                    }
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(updateReceiver);
    }
}
