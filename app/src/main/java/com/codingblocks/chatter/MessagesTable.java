package com.codingblocks.chatter;


import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class MessagesTable extends RealmObject {
    @PrimaryKey
    private int id;
    private String uId;
    private String text;
    private String timestamp;
    private String roomId;
    private String username;
    private String displayName;
    private boolean unread;
    private boolean sentStatus;
    private String userAvater;

    public int getId(){return id;}
    public String getuId(){return uId;}
    public String getRoomId(){return roomId;}
    public String getText(){return text;}
    public String getTimestamp(){return timestamp;}
    public String getUsername(){return username;}
    public String getDisplayName(){return displayName;}
    public boolean getUnread(){return unread;}
    public boolean getSentStatus(){return sentStatus;}
    public String getUserAvater(){return userAvater;}

    public void setId(int id){this.id = id;}
    public void setUId(String uId){this.uId = uId;}
    public void setText(String text){this.text = text;}
    public void setRoomId(String roomId){this.roomId = roomId;}
    public void setTimestamp(String timestamp){this.timestamp = timestamp;}
    public void setUsername(String username){this.username = username;}
    public void setDisplayName(String displayName){this.displayName = displayName;}
    public void setUnread(boolean unread){this.unread = unread;}
    public void setSentStatus(boolean sentStatus){this.sentStatus = sentStatus;}
    public void setUserAvater(String userAvater){this.userAvater=userAvater;}
}
