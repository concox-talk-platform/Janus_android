package com.example.janusandroidtalk.bean;

import java.io.Serializable;
import java.util.ArrayList;

public class UserBean implements Serializable {
    private String userName;
    private String userPhone;
    private int userId;
    private boolean userLoginState;
    private String nickName;
    private String iMei;
    private ArrayList<UserFriendBean> userFriendBeanArrayList;
    private ArrayList<UserGroupBean> userGroupBeanArrayList;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserPhone() {
        return userPhone;
    }

    public void setUserPhone(String userPhone) {
        this.userPhone = userPhone;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public boolean isUserLoginState() {
        return userLoginState;
    }

    public void setUserLoginState(boolean userLoginState) {
        this.userLoginState = userLoginState;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getiMei() {
        return iMei;
    }

    public void setiMei(String iMei) {
        this.iMei = iMei;
    }

    public ArrayList<UserFriendBean> getUserFriendBeanArrayList() {
        return userFriendBeanArrayList;
    }

    public void setUserFriendBeanArrayList(ArrayList<UserFriendBean> userFriendBeanArrayList) {
        this.userFriendBeanArrayList = userFriendBeanArrayList;
    }

    public ArrayList<UserGroupBean> getUserGroupBeanArrayList() {
        return userGroupBeanArrayList;
    }

    public void setUserGroupBeanArrayList(ArrayList<UserGroupBean> userGroupBeanArrayList) {
        this.userGroupBeanArrayList = userGroupBeanArrayList;
    }

    private static UserBean userBeanObj = null;
    public static UserBean setUserBean(UserBean userBean){
        userBeanObj = userBean;
        return userBeanObj;
    }
    public static UserBean getUserBean() {
        return userBeanObj;
    }
    public static void clearUserBean(){
        userBeanObj = null;
    }

}
