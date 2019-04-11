//package com.example.janusandroidtalk.adapter;
//
//import android.app.AlertDialog;
//import android.app.Dialog;
//import android.content.Context;
//import android.content.DialogInterface;
//import android.content.Intent;
//import android.os.Message;
//import android.view.View;
//import android.widget.ImageView;
//import android.widget.LinearLayout;
//
//import com.example.janusandroidtalk.MainActivity;
//import com.example.janusandroidtalk.MyApplication;
//import com.example.janusandroidtalk.activity.AudioTalkActivity;
//import com.example.janusandroidtalk.R;
//import com.example.janusandroidtalk.pullrecyclerview.BaseRecyclerAdapter;
//import com.example.janusandroidtalk.pullrecyclerview.BaseViewHolder;
//import com.example.janusandroidtalk.signalingcontrol.AudioBridgeControl;
//import com.example.janusandroidtalk.signalingcontrol.MyControlCallBack;
//
//import org.json.JSONException;
//import org.json.JSONObject;
//
//import java.util.List;
//import java.util.Map;
//
//public class GroupListAdapter extends BaseRecyclerAdapter implements MyControlCallBack {
//
//    private Context context;
//    public GroupListAdapter(Context context, int layoutResId, List<Map<String,String>> data) {
//        super(context, layoutResId, data);
//        this.context = context;
//    }
//
//    @Override
//    protected void converted(BaseViewHolder holder, Object item, int position) {
//        final Map<String,String> data = (Map<String, String>) item;
//        holder.setText(R.id.fragment_group_list_item_name,data.get("name"));
//        holder.setText(R.id.fragment_group_list_item_state,data.get("gid"));
//        ImageView imageView = holder.getView(R.id.fragment_group_list_item_lock);
//        LinearLayout linearLayout = holder.getView(R.id.fragment_group_list_item);
//
//        if(data.get("gid").equals(MyApplication.getDefaultGroupId()+"")){
//            imageView.setImageResource(R.drawable.ic_lock_outline_black_24dp);
//        }else{
//            imageView.setImageResource(R.drawable.ic_lock_open_black_24dp);
//        }
////
////        linearLayout.setOnClickListener(new View.OnClickListener() {
////            @Override
////            public void onClick(View v) {
////                Intent intent = new Intent(context, AudioTalkActivity.class);
////                intent.putExtra("name",data.get("name"));
////                context.startActivity(intent);
////            }
////        });
//        imageView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                final AlertDialog dialog = new AlertDialog.Builder(context)
//                        .setMessage("确定要切换到该群组吗？")
//                        .setPositiveButton("确定", new DialogInterface.OnClickListener()
//                            {
//                                @Override
//                                public void onClick(DialogInterface dialog, int which)
//                                {
//                                    //发送changeRoom信令 ,data.get("gid"),并在回调成功之后，保存默认群组Id,
//                                    int changeRoomId = Integer.parseInt(data.get("gid"));
//                                    AudioBridgeControl.sendChangeGroup(GroupListAdapter.this,changeRoomId,MyApplication.getUserId(),MyApplication.getUserName());
//                                    dialog.dismiss();
//                                }
//                            })
//                        .setNegativeButton("取消", new DialogInterface.OnClickListener()
//                            {
//                                @Override
//                                public void onClick(DialogInterface dialog, int which)
//                                {
//                                    dialog.dismiss();
//                                }
//                            }).create();
//                dialog.show();
//            }
//        });
//
//    }
//
//    @Override
//    public void showMessage(JSONObject msg) {
//        try{
//            if(msg.getString("audiobridge").equals("roomchanged")){
//                //保存新的默认群组ID
//                MyApplication.setDefaultGroupId(msg.getInt("room"));
//            }
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//
//    }
//}