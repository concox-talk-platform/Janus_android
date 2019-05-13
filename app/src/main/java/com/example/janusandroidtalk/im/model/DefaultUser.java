package com.example.janusandroidtalk.im.model;

import cn.jiguang.imui.commons.models.IUser;


public class DefaultUser implements IUser {

    private String id;//用户id
    private String displayName;//用户的名字
    private String avatar;//用户头像

    public DefaultUser(String id, String displayName, String avatar) {
        this.id = id;
        this.displayName = displayName;
        this.avatar = avatar;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getAvatarFilePath() {
        return avatar;
    }
}
