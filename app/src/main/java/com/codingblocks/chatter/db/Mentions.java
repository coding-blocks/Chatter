package com.codingblocks.chatter.db;

public class Mentions {
    private String userId;
    private String screenName;


    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getScreenName() {
        return screenName;
    }

    public void setScreenName(String screenName) {
        this.screenName = screenName;
    }


    public Mentions(String userId, String screenName) {
        this.userId = userId;
        this.screenName = screenName;
    }


}
