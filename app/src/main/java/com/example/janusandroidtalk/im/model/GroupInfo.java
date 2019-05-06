package com.example.janusandroidtalk.im.model;

import java.util.ArrayList;

public class GroupInfo {
    private int groupId;
    private String groupName;
    private String groupManager;
    private ArrayList<GroupUser> groupUser;
    private boolean isExist;

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupManager() {
        return groupManager;
    }

    public void setGroupManager(String groupManager) {
        this.groupManager = groupManager;
    }

    public ArrayList<GroupUser> getGroupUser() {
        return groupUser;
    }

    public void setGroupUser(ArrayList<GroupUser> groupUser) {
        this.groupUser = groupUser;
    }

    public boolean isExist() {
        return isExist;
    }

    public void setExist(boolean exist) {
        isExist = exist;
    }
}
