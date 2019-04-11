package com.example.janusandroidtalk.adapter;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.janusandroidtalk.R;
import com.example.janusandroidtalk.pullrecyclerview.BaseRecyclerAdapter;
import com.example.janusandroidtalk.pullrecyclerview.BaseViewHolder;


import java.util.List;
import java.util.Map;

public class MineListAdapter extends BaseRecyclerAdapter {

    private Context context;
    public MineListAdapter(Context context, int layoutResId, List<Map<String,String>> data) {
        super(context, layoutResId, data);
        this.context = context;
    }

    @Override
    protected void converted(BaseViewHolder holder, Object item, int position) {
        final Map<String,String> data = (Map<String, String>) item;
        holder.setText(R.id.fragment_mine_list_item_name,data.get("name"));
        if(data.get("state").equals("在线")){
            holder.setTextColorRes(R.id.fragment_mine_list_item_state,R.color.color_main_text);
        }else{
            holder.setTextColorRes(R.id.fragment_mine_list_item_state,R.color.black_text);
        }
        holder.setText(R.id.fragment_mine_list_item_state,data.get("state"));
        ImageView imageView = holder.getView(R.id.fragment_mine_list_item_audio_call);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context,"打电话给"+data.get("name"),Toast.LENGTH_SHORT).show();
            }
        });

    }


}