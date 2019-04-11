package com.example.janusandroidtalk.bean;

import java.io.Serializable;

public class UserFriendBean implements Serializable {
    private String userFriendName;
    private long userFriendId;
    private boolean userFriendLoginState;
    private boolean isCheck;

    public String getUserFriendName() {
        return userFriendName;
    }

    public void setUserFriendName(String userFriendName) {
        this.userFriendName = userFriendName;
    }

    public long getUserFriendId() {
        return userFriendId;
    }

    public void setUserFriendId(long userFriendId) {
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
}
