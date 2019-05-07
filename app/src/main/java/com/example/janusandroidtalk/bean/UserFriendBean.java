package com.example.janusandroidtalk.bean;

import java.io.Serializable;

import talk_cloud.TalkCloudApp;

public class UserFriendBean implements Serializable {
    private String userFriendName;
    private int userFriendId;
    private boolean userFriendLoginState;
    private int online;// 1不在线，2在线
    private int groupRole; //2 群主  1 普通群成员
    private boolean isCheck; //勾选状态
    private boolean isInGroup;

    public String getUserFriendName() {
        return userFriendName;
    }

    public void setUserFriendName(String userFriendName) {
        this.userFriendName = userFriendName;
    }

    public int getUserFriendId() {
        return userFriendId;
    }

    public void setUserFriendId(int userFriendId) {
        this.userFriendId = userFriendId;
    }

    public boolean isUserFriendLoginState() {
        return userFriendLoginState;
    }

    public void setUserFriendLoginState(boolean userFriendLoginState) {
        this.userFriendLoginState = userFriendLoginState;
    }

    public boolean isCheck() {
        return isCheck;
    }

    public void setCheck(boolean check) {
        isCheck = check;
    }

    public boolean isInGroup() {
        return isInGroup;
    }

    public void setInGroup(boolean inGroup) {
        isInGroup = inGroup;
    }

    public int getOnline() {
        return online;
    }

    public void setOnline(int online) {
        this.online = online;
    }

    public int getGroupRole() {
        return groupRole;
    }

    public void setGroupRole(int groupRole) {
        this.groupRole = groupRole;
    }

    private static UserFriendBean userFriendBeanObj = null;
    public static UserFriendBean setUserFriendBean(UserFriendBean userFriendBean){
        userFriendBeanObj = userFriendBean;
        return userFriendBeanObj;
    }
    public static UserFriendBean getUserFriendBean() {
        return userFriendBeanObj;
    }
    public static void clearUserFriendBean(){
        userFriendBeanObj = null;
    }

    public void setUserFriendBeanObjByUserRecord(TalkCloudApp.UserRecord userRecord) {
        this.userFriendName = userRecord.getName();
        this.userFriendId = userRecord.getUid();
        this.groupRole = userRecord.getGrpRole();


        if (userRecord.getUid() == UserBean.getUserBean().getUserId()) { //TODO
            this.online = 2;
        } else {
            this.online = userRecord.getOnline();
        }
        // FIXME
    }

    public void setUserFriendBeanObjByFriendRecord(TalkCloudApp.FriendRecord friendRecord) {
        this.userFriendName = friendRecord.getName();
        this.userFriendId = friendRecord.getUid();
    }
}
