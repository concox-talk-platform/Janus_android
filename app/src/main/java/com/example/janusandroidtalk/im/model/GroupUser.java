package com.example.janusandroidtalk.im.model;

public class GroupUser {

    private int userId;
    private String imei;
    private String name;
    private int online;
    private int lockGroupId;
    private int type;
    private int groupRole;
    private boolean isFriend;

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getImei() {
        return imei;
    }

    public void setImei(String imei) {
        this.imei = imei;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getOnline() {
        return online;
    }

    public void setOnline(int online) {
        this.online = online;
    }

    public int getLockGroupId() {
        return lockGroupId;
    }

    public void setLockGroupId(int lockGroupId) {
        this.lockGroupId = lockGroupId;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getGroupRole() {
        return groupRole;
    }

    public void setGroupRole(int groupRole) {
        this.groupRole = groupRole;
    }

    public boolean isFriend() {
        return isFriend;
    }

    public void setFriend(boolean friend) {
        isFriend = friend;
    }
}
