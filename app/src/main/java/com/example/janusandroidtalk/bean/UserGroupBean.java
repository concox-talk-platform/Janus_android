package com.example.janusandroidtalk.bean;

import java.io.Serializable;
import java.util.ArrayList;

public class UserGroupBean implements Serializable {
    private String userGroupName;
    private int userGroupId;
    private ArrayList<UserFriendBean> userFriendBeanArrayList;

    public String getUserGroupName() {
        return userGroupName;
    }

    public void setUserGroupName(String userGroupName) {
        this.userGroupName = userGroupName;
    }

    public int getUserGroupId() {
        return userGroupId;
    }

    public void setUserGroupId(int userGroupId) {
        this.userGroupId = userGroupId;
    }

    public ArrayList<UserFriendBean> getUserFriendBeanArrayList() {
        return userFriendBeanArrayList;
    }

    public void setUserFriendBeanArrayList(ArrayList<UserFriendBean> userFriendBeanArrayList) {
        this.userFriendBeanArrayList = userFriendBeanArrayList;
    }

    private static UserGroupBean userGroupBeanObj = null;
    public static UserGroupBean setUserGroupBean(UserGroupBean userGroupBean){
        userGroupBeanObj = userGroupBean;
        return userGroupBeanObj;
    }
    public static UserGroupBean getUserGroupBean() {
        return userGroupBeanObj;
    }
    public static void clearUserGroupBean(){
        userGroupBeanObj = null;
    }
}
