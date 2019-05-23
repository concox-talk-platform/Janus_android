package com.example.janusandroidtalk.im.record;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.janusandroidtalk.im.model.ChatMessage;
import com.example.janusandroidtalk.im.model.DefaultUser;

import java.util.ArrayList;
import java.util.List;

import cn.jiguang.imui.commons.models.IMessage;

public class ChatDBManager {

    private ChatDBHelper helper;
    private SQLiteDatabase db;

    public ChatDBManager(Context context){
         helper = new ChatDBHelper(context);
    }

    /**
     * 增加历史消息到数据库
     * @param info
     * @return
     */
    public synchronized long  addRecord(ChatMessage info){
        Log.d("chat","chatrecord addRecord send_id="+info.getFromUser().getId()+" receive_id "+info.getReceiveId());
        db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("send_id",info.getFromUser().getId());
        cv.put("send_name",info.getFromUser().getId());
        cv.put("receive_id",info.getReceiveId());
        cv.put("msgid",info.getMsgId());
        cv.put("send_time",info.getTimeString());
        cv.put("chat_content",info.getText());
        cv.put("type",info.getType());
        cv.put("status",info.getMessageStatus().ordinal());
        cv.put("mediafilepath",info.getMediaFilePath());
        cv.put("duration",info.getDuration());
        cv.put("record_timestamp", info.getTimestamp());
        long result = db.insert("chat_record",null,cv);
        Log.d("chat","chatrecord addRecord result = "+result);
        db.close();
        return result;
    }

    public void deleteAllRecord(){
        Log.d("chat","chatrecord truncate");
        db.execSQL("Delete from chat_record");
//        db.delete("chat_record",null,null);
    }

    /**
     * 查询历史消息
     * @param sendId     发送ID
     * @param receiveId  接收ID
     * @return
     */
    public List<ChatMessage> getRecord(int sendId,int receiveId){
        db = helper.getReadableDatabase();
        Log.d("chat","chatrecord sendId = "+sendId+" receiveId="+receiveId);
        ArrayList<ChatMessage> infos = new ArrayList<ChatMessage>();
//        Cursor cursor = db.rawQuery("select * from chat_record where send_id=? and receiveId=?",null,);
//        Cursor cursor = db.query("chat_record",null,"receive_id=?",new String[]{receiveId+""},null,null,null);
        Cursor cursor = db.query("chat_record",null,"send_id=? and receive_id=?", new String[]{sendId+"", receiveId+""},null,null,null);
        while (cursor.moveToNext()){
            ChatMessage info = new ChatMessage();
            info.setUserInfo(new DefaultUser(cursor.getInt(cursor.getColumnIndex("send_id"))+"",cursor.getString(cursor.getColumnIndex("send_name")),""));
            info.setReceiveId(cursor.getInt(cursor.getColumnIndex("receive_id")));
            info.setTimeString(cursor.getString(cursor.getColumnIndex("send_time")));
            info.setText(cursor.getString(cursor.getColumnIndex("chat_content")));
            info.setType(cursor.getInt(cursor.getColumnIndex("type")));
            int status = cursor.getInt(cursor.getColumnIndex("status"));
            info.setMessageStatus(IMessage.MessageStatus.values()[status]);
            info.setMediaFilePath(cursor.getString(cursor.getColumnIndex("mediafilepath")));
            info.setDuration(cursor.getLong(cursor.getColumnIndex("duration")));
            info.setTimestamp(cursor.getLong(cursor.getColumnIndex("record_timestamp")));
            infos.add(info);
            Log.d("chat","chatrecord send_timeStr="+info.getTimeString());
            Log.d("chat","chatrecord record_timestamp="+info.getTimeString());
        }
        cursor.close();
        db.close();
        return infos;
    }

    /**
     * 根据id查询消息
     * @param id
     * @return
     */
    public ChatMessage getRecordForId(long id){
        db = helper.getReadableDatabase();
        ChatMessage info = new ChatMessage();
        Cursor cursor = db.query("chat_record",null,"_id=?",new String[]{id+""},null,null,null);
        while (cursor.moveToNext()){
            info.setUserInfo(new DefaultUser(cursor.getInt(cursor.getColumnIndex("send_id"))+"",cursor.getString(cursor.getColumnIndex("send_name")),""));
            info.setReceiveId(cursor.getInt(cursor.getColumnIndex("receive_id")));
            info.setTimeString(cursor.getString(cursor.getColumnIndex("send_time")));
            info.setText(cursor.getString(cursor.getColumnIndex("chat_content")));
            info.setType(cursor.getInt(cursor.getColumnIndex("type")));
            int status = cursor.getInt(cursor.getColumnIndex("status"));
            info.setMessageStatus(IMessage.MessageStatus.values()[status]);
            info.setMediaFilePath(cursor.getString(cursor.getColumnIndex("mediafilepath")));
            info.setDuration(cursor.getLong(cursor.getColumnIndex("duration")));
            info.setTimestamp(cursor.getLong(cursor.getColumnIndex("record_timestamp")));
            Log.d("chat","chatrecord send_timeStr="+info.getTimeString());
            Log.d("chat","chatrecord record_timestamp="+info.getTimeString());
            //return info;
        }
        cursor.close();
        db.close();
        return info;
    }

    /**
     * 根据消息message id查询消息
     * @param id
     * @return
     */
    public ChatMessage getRecordForMsgId(String id){
        db = helper.getReadableDatabase();
        ChatMessage info = new ChatMessage();
        Cursor cursor = db.query("chat_record",null,"msgid=?",new String[]{id+""},null,null,null);
        while (cursor.moveToNext()){
            info.setUserInfo(new DefaultUser(cursor.getInt(cursor.getColumnIndex("send_id"))+"",cursor.getString(cursor.getColumnIndex("send_name")),""));
            info.setReceiveId(cursor.getInt(cursor.getColumnIndex("receive_id")));
            info.setTimeString(cursor.getString(cursor.getColumnIndex("send_time")));
            info.setText(cursor.getString(cursor.getColumnIndex("chat_content")));
            info.setType(cursor.getInt(cursor.getColumnIndex("type")));
            int status = cursor.getInt(cursor.getColumnIndex("status"));
            info.setMessageStatus(IMessage.MessageStatus.values()[status]);
            info.setMediaFilePath(cursor.getString(cursor.getColumnIndex("mediafilepath")));
            info.setDuration(cursor.getLong(cursor.getColumnIndex("duration")));
            info.setTimestamp(cursor.getLong(cursor.getColumnIndex("record_timestamp")));
            Log.d("chat","chatrecord send_timeStr="+info.getTimeString());
            Log.d("chat","chatrecord record_timestamp="+info.getTimeString());
            //return info;
        }
        cursor.close();
        db.close();
        return info;
    }

    /**
     * 修改消息状态
     * @param msgid
     * @param status
     * @return
     */
    public int modifyRecordMsg(String msgid,int status){
        Log.d("chat","chatrecord modifyRecordMsg msgid="+msgid);
        db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("status",status);
        int line = db.update("chat_record",cv,"msgid=?",new String[]{msgid});
        db.close();
        return line;
    }
    public void closeDB(){
        db.close();
    }

}
