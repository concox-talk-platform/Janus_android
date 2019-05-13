package com.example.janusandroidtalk.im.utils;

import android.media.MediaPlayer;
import android.net.Uri;

import com.example.janusandroidtalk.MyApplication;

import java.io.File;
import java.io.IOException;

public class MediaUtil {

    /**
     * 获取语音、视频的时长
     * @param source   文件的路径
     * @return
     */
    public static int getDuration(File source) {
        int duration = 0;
        Uri uri = Uri.fromFile(source);
        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(MyApplication.getContext(), uri);
            mediaPlayer.prepare();
            duration = mediaPlayer.getDuration();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return duration / 1000;

    }

}
