package com.example.janusandroidtalk.floatwindow.view;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.example.janusandroidtalk.MyApplication;
import com.example.janusandroidtalk.R;
import com.example.janusandroidtalk.floatwindow.FloatActionController;
import com.example.janusandroidtalk.signalingcontrol.JanusControl;
import com.example.janusandroidtalk.signalingcontrol.MyControlCallBack;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.MediaStream;

/**
 * Author:xishuang
 * Date:2017.08.01
 * Des:悬浮窗的布局
 */
public class FloatLayout extends FrameLayout implements MyControlCallBack {
    private final WindowManager mWindowManager;
    private final ImageView mFloatView;
    private float mTouchStartX;
    private float mTouchStartY;
    private WindowManager.LayoutParams mWmParams;
    private Context mContext;

    public FloatLayout(Context context) {
        this(context, null);
        mContext = context;
    }

    public FloatLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        LayoutInflater.from(context).inflate(R.layout.float_syspend_logo, this);
        //浮动窗口按钮
        mFloatView = (ImageView) findViewById(R.id.iv_logo);

        FloatActionController.getInstance().setObtainNumber(1);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 获取相对屏幕的坐标，即以屏幕左上角为原点
        int x = (int) event.getRawX();
        int y = (int) event.getRawY();
        //下面的这些事件，跟图标的移动无关，为了区分开拖动和点击事件
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mTouchStartX = event.getX();
                mTouchStartY = event.getY();
                mFloatView.setImageResource(R.mipmap.audio_gray);
                JanusControl.sendTalk(FloatLayout.this);
                break;
            case MotionEvent.ACTION_MOVE:
                // 更新浮动窗口位置参数
                mWmParams.x = (int) (x - mTouchStartX);
                mWmParams.y = (int) (y - mTouchStartY);
                mWindowManager.updateViewLayout(this, mWmParams);
                break;
            case MotionEvent.ACTION_UP:
                JanusControl.sendConfigure(FloatLayout.this,true);
                JanusControl.sendUnTalk(FloatLayout.this);
                break;
        }
        //响应点击事件
        return true;
    }

    /**
     * 将小悬浮窗的参数传入，用于更新小悬浮窗的位置。
     *
     * @param params 小悬浮窗的参数
     */
    public void setParams(WindowManager.LayoutParams params) {
        mWmParams = params;
    }


    @Override
    public void janusServer(int code,String msg) {

    }

    //回调信息处理，
    @Override
    public void showMessage(final JSONObject msg,JSONObject jsepLocal) {
        try{
            if(msg.getString("pocroom").equals("talked")){
                if(msg.getInt("id") == 0 || (msg.getInt("id") == MyApplication.getUserId()) ){
                    Message message1 = new Message();
                    message1.what = 1;
                    handler.sendMessage(message1);
                }else{
                    Message message2 = new Message();
                    message2.what = 2;
                    handler.sendMessage(message2);
                }
            }else if(msg.getString("pocroom").equals("untalked")){
                Message message3 = new Message();
                message3.what = 3;
                handler.sendMessage(message3);
            }else if(msg.getString("pocroom").equals("configured")){
//               if(msg.getBoolean("muted")){
//                   JanusControl.sendUnTalk(FloatLayout.this);
//               }
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
                    JanusControl.sendConfigure(FloatLayout.this,false);
                    break;
                case 2:

                    break;
                case 3:
                    mFloatView.setImageResource(R.mipmap.audio_green);
                    break;
            }
        };
    };
}
