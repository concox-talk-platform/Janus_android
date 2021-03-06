package com.example.janusandroidtalk.bean;

import java.io.Serializable;
import java.util.ArrayList;

import talk_cloud.TalkCloudApp;

public class UserGroupBean implements Serializable {
    private String userGroupName;
    private int userGroupId;
    private ArrayList<UserFriendBean> userFriendBeanArrayList = new ArrayList<>();
//    private int userGroupRole;//群主为2,其他参与者为1

    // Group manager
    private int groupManagerId;

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

//    public int getUserGroupRole() {
//        return userGroupRole;
//    }
//
//    public void setUserGroupRole(int userGroupRole) {
//        this.userGroupRole = userGroupRole;
//    }

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

    public void setGroupManagerId(int groupManagerId) {
        this.groupManagerId = groupManagerId;
    }

    public int getGroupManagerId() {
        return groupManagerId;
    }

    public int getTotalMembersCount() {
        return userFriendBeanArrayList.size();
    }

    public int getOnlineMembersCountLocal() {
        int count = 0;

        for (UserFriendBean userFriendBean : userFriendBeanArrayList) {
            if (userFriendBean.getOnline() == 2) {
                count++;
            }
        }

        return count;
    }

    public void setUserGroupBeanObj(TalkCloudApp.GroupInfo groupInfo) {
        this.userGroupId = groupInfo.getGid();
        this.userGroupName = groupInfo.getGroupName();
        this.groupManagerId = groupInfo.getGroupManager();

        ArrayList<UserFriendBean> memberList = new ArrayList<>();
        for (TalkCloudApp.UserRecord userRecord : groupInfo.getUsrListList()) {
            UserFriendBean userFriendBean = new UserFriendBean();
            userFriendBean.setUserFriendBeanObjByUserRecord(userRecord);

            memberList.add(userFriendBean);
        }

        this.userFriendBeanArrayList.addAll(memberList);
    }
}
